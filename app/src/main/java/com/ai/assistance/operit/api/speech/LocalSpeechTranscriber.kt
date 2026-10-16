package com.ai.assistance.operit.api.speech

import android.content.Context
import android.os.SystemClock
import com.ai.assistance.operit.core.tools.FileContentData
import com.ai.assistance.operit.data.model.ToolResult
import com.ai.assistance.operit.data.speech.ParakeetModelStorage
import com.ai.assistance.operit.data.speech.SttModelStorage
import com.ai.assistance.operit.util.AppLogger

/**
 * Feeds an audio file through the same on-device pipeline the microphone
 * uses: resample to 16 kHz, then either a 512-sample VAD walk with utterance
 * chunking ([SttFileMode.CHUNK]) or one decode over the whole file
 * ([SttFileMode.WHOLE]), with the engine picked by [SttFileEngine].
 *
 * Exists so a benchmark WAV can be scored through the production graphs
 * without a microphone or a speaker. No silent conversions: unsupported
 * files and a missing model fail loudly.
 */
enum class SttFileEngine { GIGAAM, PARAKEET }

enum class SttFileMode { CHUNK, WHOLE }

data class FileTranscription(
    val text: String,
    val chunks: Int,
    val audioSeconds: Double,
    val decodeMs: Long,
)

private const val TAG = "LocalSpeechTranscriber"
private const val TARGET_RATE = 16000
private const val VAD_FRAME = 512

fun transcribeWavFile(
    context: Context,
    path: String,
    engine: SttFileEngine = SttFileEngine.GIGAAM,
    mode: SttFileMode = SttFileMode.CHUNK,
): FileTranscription {
    val wav = WavFileReader.read(path)
    val resampler = PcmResampler(wav.sampleRateHz, TARGET_RATE)
    val pcm16k: ShortArray = if (resampler.isPassthrough) {
        wav.samples
    } else {
        val head = resampler.processInt16(wav.samples)
        val tail = resampler.flush()
        val tail16 = ShortArray(tail.size) {
            (tail[it] * 32768.0).toInt().coerceIn(-32768, 32767).toShort()
        }
        head + tail16
    }

    val started = SystemClock.elapsedRealtime()
    val result = when (engine) {
        SttFileEngine.GIGAAM -> {
            val modelDir = SttModelStorage(context).isDownloaded()
                ?: throw IllegalStateException("offline STT model is not downloaded")
            GigaAMRecognizer.open(modelDir).use { recognizer ->
                runEngine(context, pcm16k, mode, wholeLimitSeconds = null, recognizer::transcribe)
            }
        }
        SttFileEngine.PARAKEET -> {
            val modelDir = ParakeetModelStorage(context).isDownloaded()
                ?: throw IllegalStateException("offline Parakeet model is not downloaded")
            ParakeetRecognizer.open(modelDir).use { recognizer ->
                runEngine(
                    context,
                    pcm16k,
                    mode,
                    // One production decode never exceeds the 20 s utterance
                    // ceiling, and a longer single encode does not fit this
                    // phone's memory (LMK kill, no error). Longer files must
                    // go through chunk mode.
                    wholeLimitSeconds = 20,
                    recognizer::transcribe,
                )
            }
        }
    }
    val decodeMs = SystemClock.elapsedRealtime() - started
    val audioSeconds = pcm16k.size.toDouble() / TARGET_RATE
    AppLogger.d(
        TAG,
        "transcribed $path ($engine/$mode): ${result.parts.size} non-blank " +
            "of ${result.chunks} chunks, %.1f s audio in %d ms".format(audioSeconds, decodeMs),
    )
    return FileTranscription(result.parts.joinToString(" "), result.chunks, audioSeconds, decodeMs)
}

private data class EngineRun(val parts: List<String>, val chunks: Int)

private fun runEngine(
    context: Context,
    pcm16k: ShortArray,
    mode: SttFileMode,
    wholeLimitSeconds: Long?,
    transcribe: (ShortArray) -> String,
): EngineRun {
    if (mode == SttFileMode.WHOLE) {
        val audioSeconds = pcm16k.size / TARGET_RATE.toDouble()
        if (wholeLimitSeconds != null && audioSeconds > wholeLimitSeconds) {
            throw IllegalStateException(
                "file is %.1f s, whole mode supports up to %d s on this phone; use chunk mode".format(
                    audioSeconds,
                    wholeLimitSeconds,
                ),
            )
        }
        val text = transcribe(pcm16k)
        return EngineRun(if (text.isBlank()) emptyList() else listOf(text), 1)
    }
    val parts = ArrayList<String>()
    var chunks = 0
    OnnxSileroVad(context).use { vad ->
        val chunker = VadUtteranceChunker()
        var pos = 0
        while (pos < pcm16k.size) {
            val end = (pos + VAD_FRAME).coerceAtMost(pcm16k.size)
            val frame = if (end - pos == VAD_FRAME) {
                pcm16k.copyOfRange(pos, end)
            } else {
                ShortArray(VAD_FRAME).also {
                    pcm16k.copyInto(it, 0, pos, end)
                }
            }
            val done = chunker.feed(frame, vad.isSpeech(frame))
            if (done != null) {
                val text = transcribe(done)
                if (text.isNotBlank()) parts.add(text)
                chunks++
            }
            pos = end
        }
        val tail = chunker.flush()
        if (tail != null) {
            val text = transcribe(tail)
            if (text.isNotBlank()) parts.add(text)
            chunks++
        }
    }
    return EngineRun(parts, chunks)
}

/** Tool-facing wrapper: pure transcript in, errors out as exceptions. */
fun transcribeLocalSpeechFile(
    context: Context,
    path: String,
    engine: SttFileEngine = SttFileEngine.GIGAAM,
    mode: SttFileMode = SttFileMode.CHUNK,
): ToolResult {
    val result = transcribeWavFile(context, path, engine, mode)
    val text = result.text.ifBlank { "No speech detected in file." }
    return ToolResult(
        toolName = "transcribe_file",
        success = true,
        result = FileContentData(
            path = path,
            content = text,
            size = text.length.toLong(),
        ),
        error = "",
    )
}
