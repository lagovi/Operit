package com.ai.assistance.operit.api.speech

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.ai.assistance.operit.data.speech.GigaAMModelFiles
import com.ai.assistance.operit.util.AppLogger
import java.io.Closeable
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Runs the GigaAM-Multilingual CTC graph over one utterance.
 *
 * The graph is used directly through onnxruntime rather than through sherpa-onnx
 * JNI, because the sherpa-onnx `.so` files are only distributed inside an
 * upstream Google Drive archive. onnxruntime is already a dependency and
 * `OnnxSileroVad` already uses it.
 *
 * Input and output names are resolved from the session rather than hardcoded,
 * with the names this export is known to use as the preferred spelling, so a
 * differently named graph fails loudly at load time rather than decoding noise.
 */
class GigaAMRecognizer private constructor(
    private val session: OrtSession,
    private val vocabulary: GigaAMTokenVocabulary,
    private val featuresInput: String,
    private val lengthsInput: String,
    private val logProbsOutput: String,
    private val config: CtcModelConfig = CtcModelConfig.GIGAAM_DEFAULT,
) : Closeable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val frontend = LogMelFrontend(config)

    val vocabSize: Int get() = vocabulary.size

    /** Encode mono PCM at [CtcModelConfig.sampleRate] into text. */
    fun transcribe(pcm: ShortArray): String {
        val frames = frontend.frameCount(pcm.size)
        if (frames == 0) return ""
        val features = frontend.extract(pcm)
        return transcribeFeatures(features, frames)
    }

    /**
     * @param features mel-major [GigaAMFeatureExtractor.N_MELS] * frames, as
     *   produced by `extract`. Exposed so a caller that already has the
     *   spectrogram does not recompute it.
     */
    fun transcribeFeatures(features: FloatArray, frames: Int): String {
        val logProbs = run(features, frames) ?: return ""
        val encodedFrames = encodedFramesFor(frames)
        return vocabulary.greedyDecode(logProbs, encodedFrames, vocabSize)
    }

    /** Exposed so tests can assert against the NumPy oracle without re-decoding. */
    internal fun run(features: FloatArray, frames: Int): FloatArray? {
        require(features.size == config.nMels * frames) {
            "expected ${config.nMels * frames} feature values for " +
                "$frames frames, got ${features.size}"
        }
        val featureTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(features),
            longArrayOf(1, config.nMels.toLong(), frames.toLong())
        )
        val lengthTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(longArrayOf(frames.toLong())),
            longArrayOf(1)
        )
        // Named parameters, not the implicit `it`: nesting two `use` blocks makes
        // the inner `it` shadow the outer one, which silently binds the int64
        // length tensor to the float features input and fails at run() time.
        featureTensor.use { features ->
            lengthTensor.use { lengths ->
                session.run(mapOf(featuresInput to features, lengthsInput to lengths)).use { out ->
                    // OrtSession.Result.get(String) is Optional-typed.
                    val value = out[logProbsOutput].orElse(null)?.value
                    return flattenLogProbs(value)
                }
            }
        }
    }

    /**
     * Flatten the graph's `log_probs` output, declared `[batch, frames, vocab]`,
     * into `frames * vocab` values. Taking `value[0][0]` alone would hand the
     * decoder only the first timestep.
     *
     * @return null when the runtime hands back something that is not a 3-D tensor,
     *   which would otherwise be indistinguishable from silence.
     */
    private fun flattenLogProbs(value: Any?): FloatArray? {
        val batch = value as? Array<*> ?: return null
        val frames = batch.firstOrNull() as? Array<*> ?: return null
        val flat = FloatArray(frames.size * vocabSize)
        var at = 0
        for (row in frames) {
            val floats = row as? FloatArray ?: return null
            require(floats.size == vocabSize) {
                "log_probs row has ${floats.size} classes, vocabulary has $vocabSize"
            }
            floats.copyInto(flat, at)
            at += floats.size
        }
        return flat
    }

    /**
     * The subsampling front-end reduces the frame rate, so the decoded sequence
     * is shorter than the input by [CtcModelConfig.subsamplingFactor].
     * `encoded_lengths` in the graph's output is authoritative when available.
     */
    private fun encodedFramesFor(frames: Int): Int =
        ((frames + config.subsamplingFactor - 1) / config.subsamplingFactor).coerceAtLeast(1)

    override fun close() {
        runCatching { session.close() }
            .onFailure { AppLogger.e(TAG, "Failed to close the GigaAM session", it) }
    }

    companion object {
        private const val TAG = "GigaAMRecognizer"
        const val SUBSAMPLING_FACTOR = 4

        /**
         * @param modelDir directory holding [GigaAMModelFiles.MODEL_FILE_NAME] and
         *   [GigaAMModelFiles.TOKENS_FILE_NAME], both already verified.
         */
        fun open(modelDir: File, threads: Int = DEFAULT_THREADS): GigaAMRecognizer {
            val modelFile = File(modelDir, GigaAMModelFiles.MODEL_FILE_NAME)
            val tokensFile = File(modelDir, GigaAMModelFiles.TOKENS_FILE_NAME)
            return openCustom(
                modelFile = modelFile,
                tokensFile = tokensFile,
                config = CtcModelConfig.GIGAAM_DEFAULT,
                threads = threads,
            )
        }

        /**
         * A checkpoint with its own acoustic numbers, e.g. added from a
         * Hugging Face link. Verification, if any, is the caller's job;
         * opening only fails loudly on unreadable files and unusable graphs.
         */
        fun openCustom(
            modelFile: File,
            tokensFile: File,
            config: CtcModelConfig,
            threads: Int = DEFAULT_THREADS,
        ): GigaAMRecognizer {
            require(modelFile.isFile) { "missing ${modelFile.absolutePath}" }
            require(tokensFile.isFile) { "missing ${tokensFile.absolutePath}" }

            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(threads.coerceAtLeast(1))
                setInterOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            val env = OrtEnvironment.getEnvironment()
            val session = try {
                env.createSession(modelFile.absolutePath, options)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Cannot open ${modelFile.name}", e)
                throw e
            }
            try {
                // parse() closes the reader itself; wrapping it in another use{} double-closes.
                val vocabulary = GigaAMTokenVocabulary.parse(
                    tokensFile.inputStream(),
                    blankOverride = config.blankId,
                )
                return GigaAMRecognizer(
                    session = session,
                    vocabulary = vocabulary,
                    featuresInput = resolve(session.inputNames.toList(), "features", "input"),
                    lengthsInput = resolve(
                        session.inputNames.toList(),
                        "feature_lengths",
                        "lengths",
                        "length"
                    ),
                    logProbsOutput = resolve(session.outputNames.toList(), "log_probs", "logits", "output"),
                    config = config,
                )
            } catch (e: Exception) {
                session.close()
                throw e
            }
        }

        private fun resolve(available: List<String>, vararg preferred: String): String {
            for (name in preferred) if (available.contains(name)) return name
            throw IllegalStateException(
                "none of ${preferred.toList()} are among the graph's names $available"
            )
        }

        private const val DEFAULT_THREADS = 2
    }
}