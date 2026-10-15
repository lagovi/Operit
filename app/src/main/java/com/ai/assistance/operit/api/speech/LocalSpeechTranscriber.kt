package com.ai.assistance.operit.api.speech

import android.content.Context
import android.os.SystemClock
import com.ai.assistance.operit.core.tools.FileContentData
import com.ai.assistance.operit.data.model.ToolResult
import com.ai.assistance.operit.data.speech.SttModelStorage
import com.ai.assistance.operit.util.AppLogger

/**
 * Feeds an audio file through the same on-device pipeline the microphone
 * uses: resample to 16 kHz, 512-sample VAD walk, utterance chunking, one
 * [GigaAMRecognizer.transcribe] per chunk.
 *
 * Exists so a benchmark WAV can be scored through the production graph
 * without a microphone or a speaker. No silent conversions: unsupported
 * files and a missing model fail loudly.
 */
data class FileTranscription(
    val text: String,
    val chunks: Int,
    val audioSeconds: Double,
    val decodeMs: Long,
)

private const val TAG = "LocalSpeechTranscriber"
private const val TARGET_RATE = 16000
private const val VAD_FRAME = 512

fun transcribeWavFile(context: Context, path: String): FileTranscription {
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

    val modelDir = SttModelStorage(context).isDownloaded()
        ?: throw IllegalStateException("offline STT model is not downloaded")
    val started = SystemClock.elapsedRealtime()
    val parts = ArrayList<String>()
    var chunks = 0
    GigaAMRecognizer.open(modelDir).use { recognizer ->
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
                    val text = recognizer.transcribe(done)
                    if (text.isNotBlank()) parts.add(text)
                    chunks++
                }
                pos = end
            }
            val tail = chunker.flush()
            if (tail != null) {
                val text = recognizer.transcribe(tail)
                if (text.isNotBlank()) parts.add(text)
                chunks++
            }
        }
    }
    val decodeMs = SystemClock.elapsedRealtime() - started
    val audioSeconds = pcm16k.size.toDouble() / TARGET_RATE
    AppLogger.d(
        TAG,
        "transcribed $path: ${parts.size} non-blank of $chunks chunks, " +
            "%.1f s audio in %d ms".format(audioSeconds, decodeMs),
    )
    return FileTranscription(parts.joinToString(" "), chunks, audioSeconds, decodeMs)
}

/** Tool-facing wrapper: pure transcript in, errors out as exceptions. */
fun transcribeLocalSpeechFile(context: Context, path: String): ToolResult {
    val result = transcribeWavFile(context, path)
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
