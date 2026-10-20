package com.ai.assistance.operit.api.speech

import com.ai.assistance.operit.data.speech.NemotronModelFiles
import com.ai.assistance.operit.util.AppLogger
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SherpaOnnxNative
import java.io.Closeable
import java.io.File

/**
 * Runs Nemotron 3.5 ASR Streaming 0.6B (int8, 1120 ms) through sherpa-onnx.
 *
 * The graph is a streaming transducer: one `OnlineStream` per utterance,
 * waveform in, greedy decode, result out. This class deliberately does one
 * utterance at a time — the caller (the provider or the file transcriber)
 * owns the VAD chunking, exactly like the Parakeet engine, so both paths
 * share the same phrase boundaries.
 *
 * Language is a per-stream option (`stream.setOption("language", …)`), so one
 * recognizer instance serves every language and no reload is needed to switch.
 * The runtime is the on-demand arm64 `libsherpa-onnx-jni.so` extracted from
 * the official AAR; [SherpaOnnxNative.ensureLoaded] must run before the first
 * `OnlineRecognizer` is constructed (the vendored API cannot use
 * `System.loadLibrary` because the library is not in the APK).
 */
class NemotronRecognizer private constructor(
    private val recognizer: OnlineRecognizer,
) : Closeable {

    /**
     * Transcribe one utterance of mono PCM16 at 16 kHz.
     *
     * @param language base code understood by the model (`en`, `ru`, `uk`, …
     *   or `auto`); empty means the model's own auto detection
     */
    fun transcribe(pcm: ShortArray, language: String): String {
        if (pcm.isEmpty()) return ""

        val samples = FloatArray(pcm.size) { pcm[it] / 32768.0f }
        val stream = recognizer.createStream()
        try {
            if (language.isNotEmpty()) {
                stream.setOption("language", language)
            }
            stream.acceptWaveform(samples, TARGET_RATE)
            stream.inputFinished()
            while (recognizer.isReady(stream)) {
                recognizer.decode(stream)
            }
            return recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    override fun close() {
        try {
            recognizer.release()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "NemotronRecognizer"

        /** What the model eats: 16 kHz mono PCM16. */
        const val TARGET_RATE = 16000

        /** Measured best on the 8-core test phone (HANDOFF E28). */
        const val DEFAULT_THREADS = 4

        /**
         * The stream option value that lets the model pick the language from
         * the audio. Used when the caller has no preference to impose (file
         * transcription) and for the entry shown at the top of the language
         * list.
         */
        const val AUTO_LANGUAGE = "auto"

        /** Base language codes the model accepts on the stream option. */
        val SUPPORTED_LANGUAGES = listOf(
            AUTO_LANGUAGE, "en", "ru", "uk", "de", "es", "fr", "it", "pt", "nl",
            "tr", "ar", "hi", "ja", "ko", "vi",
        )

        /**
         * @param modelDir directory with `encoder.int8.onnx`, `decoder.int8.onnx`,
         *   `joiner.int8.onnx` and `tokens.txt`, all already verified
         * @param runtimeLib the extracted arm64 `libsherpa-onnx-jni.so`
         */
        fun open(
            modelDir: File,
            runtimeLib: File,
            threads: Int = DEFAULT_THREADS,
        ): NemotronRecognizer {
            val encoderFile = File(modelDir, NemotronModelFiles.ENCODER_FILE_NAME)
            val decoderFile = File(modelDir, NemotronModelFiles.DECODER_FILE_NAME)
            val joinerFile = File(modelDir, NemotronModelFiles.JOINER_FILE_NAME)
            val tokensFile = File(modelDir, NemotronModelFiles.TOKENS_FILE_NAME)
            require(encoderFile.isFile) { "missing ${encoderFile.absolutePath}" }
            require(decoderFile.isFile) { "missing ${decoderFile.absolutePath}" }
            require(joinerFile.isFile) { "missing ${joinerFile.absolutePath}" }
            require(tokensFile.isFile) { "missing ${tokensFile.absolutePath}" }

            SherpaOnnxNative.ensureLoaded(runtimeLib)

            val config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = TARGET_RATE, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = encoderFile.absolutePath,
                        decoder = decoderFile.absolutePath,
                        joiner = joinerFile.absolutePath,
                    ),
                    tokens = tokensFile.absolutePath,
                    numThreads = threads.coerceAtLeast(1),
                    provider = "cpu",
                    debug = false,
                ),
                decodingMethod = "greedy_search",
            )
            return try {
                NemotronRecognizer(OnlineRecognizer(config = config))
            } catch (e: Exception) {
                AppLogger.e(TAG, "Cannot create the sherpa-onnx recognizer", e)
                throw e
            }
        }
    }
}
