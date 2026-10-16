package com.ai.assistance.operit.api.speech

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.ai.assistance.operit.data.speech.ParakeetModelFiles
import com.ai.assistance.operit.util.AppLogger
import java.io.Closeable
import java.io.File
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

/**
 * Runs parakeet-tdt-0.6b-v3 (int8) through onnxruntime directly.
 *
 * Two sessions: the FastConformer encoder (`encoder-model.int8.onnx`) and
 * the fused predictor+joint (`decoder_joint-model.int8.onnx`). The decode
 * is the greedy TDT loop from the onnx-asr reference: per encoder frame
 * run the joint with the previous token and predictor states; the joint
 * returns 8193 token logits plus 5 duration logits; blanks advance by
 * `max(duration, 1)` without touching the states, emissions keep the new
 * states, at most [MAX_SYMBOLS_PER_FRAME] emissions stay on one frame.
 *
 * Input/output names are resolved from the sessions with the spellings the
 * NeMo export is known to use, so a differently shaped graph fails loudly
 * at open time rather than decoding noise.
 */
class ParakeetRecognizer private constructor(
    private val encoder: OrtSession,
    private val joint: OrtSession,
    private val vocabulary: ParakeetVocabulary,
    private val audioInput: String,
    private val lengthInput: String,
    private val encoderOutput: String,
    private val encoderLengthsOutput: String,
    private val frameInput: String,
    private val targetsInput: String,
    private val targetLengthInput: String,
    private val statesIn1: String,
    private val statesIn2: String,
    private val logitsOutput: String,
    private val statesOut1: String,
    private val statesOut2: String,
) : Closeable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    val vocabSize: Int get() = vocabulary.size

    /** Encode mono PCM16 at 16 kHz into text, one full pass. */
    fun transcribe(pcm: ShortArray): String {
        val features = ParakeetFrontend.extract(pcm)
        if (features.validFrames == 0) return ""
        val encoded = encode(features)
        if (encoded.frames == 0) return ""
        val ids = decodeGreedy(encoded)
        return vocabulary.decode(ids)
    }

    private data class Encoded(val frames: FloatArray, val count: Int)

    private fun encode(features: ParakeetFrontend.Features): Encoded {
        val audioTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(features.melMajor),
            longArrayOf(1, ParakeetFrontend.N_MELS.toLong(), features.frames.toLong()),
        )
        val lengthTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(longArrayOf(features.validFrames.toLong())),
            longArrayOf(1),
        )
        try {
            encoder.run(mapOf(audioInput to audioTensor, lengthInput to lengthTensor)).use { out ->
                val raw = out[encoderOutput].get().value as Array<Array<FloatArray>>
                val lens = out[encoderLengthsOutput].get().value as LongArray
                val dim1 = raw.size
                val dim2 = raw[0].size
                val dim3 = raw[0][0].size
                // Raw layout is [1, 1024, F]; transpose defensively in case an
                // export stores [1, F, 1024] instead.
                val frames: FloatArray
                val count: Int
                if (dim1 == 1 && dim2 == ENC_DIM && dim3 != ENC_DIM) {
                    // [1, 1024, F]: frame t is column t.
                    count = lens[0].toInt().coerceIn(0, dim3)
                    frames = FloatArray(count * ENC_DIM)
                    for (t in 0 until count) {
                        for (d in 0 until ENC_DIM) frames[t * ENC_DIM + d] = raw[0][d][t]
                    }
                } else if (dim1 == 1 && dim3 == ENC_DIM && dim2 != ENC_DIM) {
                    // [1, F, 1024]: frame t is row t.
                    count = lens[0].toInt().coerceIn(0, dim2)
                    frames = FloatArray(count * ENC_DIM)
                    for (t in 0 until count) {
                        System.arraycopy(raw[0][t], 0, frames, t * ENC_DIM, ENC_DIM)
                    }
                } else {
                    throw IllegalStateException(
                        "unexpected encoder output shape [$dim1, $dim2, $dim3]",
                    )
                }
                return Encoded(frames, count)
            }
        } finally {
            audioTensor.close()
            lengthTensor.close()
        }
    }

    private fun decodeGreedy(encoded: Encoded): List<Int> {
        val ids = ArrayList<Int>()
        var states = Array(2) { FloatArray(PRED_LAYERS * PRED_HIDDEN) }
        var prev = vocabulary.blankId
        var t = 0
        var emitted = 0
        while (t < encoded.count) {
            val frame = FloatArray(ENC_DIM)
            System.arraycopy(encoded.frames, t * ENC_DIM, frame, 0, ENC_DIM)
            val (token, duration, next) = stepJoint(frame, prev, states)
            if (token != vocabulary.blankId) {
                states = next
                prev = token
                ids.add(token)
                emitted++
            }
            if (duration > 0) {
                t += duration
                emitted = 0
            } else if (token == vocabulary.blankId || emitted >= MAX_SYMBOLS_PER_FRAME) {
                t += 1
                emitted = 0
            }
        }
        return ids
    }

    private data class JointStep(
        val token: Int,
        val duration: Int,
        val states: Array<FloatArray>,
    )

    private fun stepJoint(
        frame: FloatArray,
        prev: Int,
        states: Array<FloatArray>,
    ): JointStep {
        val frameTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(frame),
            longArrayOf(1, ENC_DIM.toLong(), 1),
        )
        val targetsTensor = OnnxTensor.createTensor(
            env,
            IntBuffer.wrap(intArrayOf(prev)),
            longArrayOf(1, 1),
        )
        val targetLengthTensor = OnnxTensor.createTensor(
            env,
            IntBuffer.wrap(intArrayOf(1)),
            longArrayOf(1),
        )
        val stateTensors = states.map { s ->
            OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(s),
                longArrayOf(PRED_LAYERS.toLong(), 1, PRED_HIDDEN.toLong()),
            )
        }
        try {
            val inputs = linkedMapOf<String, OnnxTensor>(
                frameInput to frameTensor,
                targetsInput to targetsTensor,
                targetLengthInput to targetLengthTensor,
                statesIn1 to stateTensors[0],
                statesIn2 to stateTensors[1],
            )
            joint.run(inputs).use { out ->
                @Suppress("UNCHECKED_CAST")
                val raw = out[logitsOutput].get().value as Array<Array<Array<FloatArray>>>
                val logits = raw[0][0][0]
                var token = 0
                var best = logits[0]
                for (i in 1 until vocabulary.size) {
                    if (logits[i] > best) {
                        best = logits[i]
                        token = i
                    }
                }
                var duration = 0
                var bestDur = logits[vocabulary.size]
                for (d in 1 until N_DURATIONS) {
                    if (logits[vocabulary.size + d] > bestDur) {
                        bestDur = logits[vocabulary.size + d]
                        duration = d
                    }
                }
                @Suppress("UNCHECKED_CAST")
                val nextStates = Array(2) { k ->
                    val name = if (k == 0) statesOut1 else statesOut2
                    val flat = out[name].get().value as Array<Array<FloatArray>>
                    FloatArray(PRED_LAYERS * PRED_HIDDEN).also { dst ->
                        for (l in 0 until PRED_LAYERS) {
                            System.arraycopy(flat[l][0], 0, dst, l * PRED_HIDDEN, PRED_HIDDEN)
                        }
                    }
                }
                return JointStep(token, duration, nextStates)
            }
        } finally {
            frameTensor.close()
            targetsTensor.close()
            targetLengthTensor.close()
            stateTensors.forEach { it.close() }
        }
    }

    override fun close() {
        try {
            encoder.close()
        } catch (_: Exception) {
        }
        try {
            joint.close()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "ParakeetRecognizer"
        private const val ENC_DIM = 1024
        private const val PRED_LAYERS = 2
        private const val PRED_HIDDEN = 640
        private const val N_DURATIONS = 5
        private const val MAX_SYMBOLS_PER_FRAME = 10
        private const val DEFAULT_THREADS = 2

        /**
         * @param modelDir directory holding [ParakeetModelFiles.ENCODER_FILE_NAME],
         *   [ParakeetModelFiles.JOINT_FILE_NAME] and
         *   [ParakeetModelFiles.VOCAB_FILE_NAME], all already verified.
         */
        fun open(modelDir: File, threads: Int = DEFAULT_THREADS): ParakeetRecognizer {
            val encoderFile = File(modelDir, ParakeetModelFiles.ENCODER_FILE_NAME)
            val jointFile = File(modelDir, ParakeetModelFiles.JOINT_FILE_NAME)
            val vocabFile = File(modelDir, ParakeetModelFiles.VOCAB_FILE_NAME)
            require(encoderFile.isFile) { "missing ${encoderFile.absolutePath}" }
            require(jointFile.isFile) { "missing ${jointFile.absolutePath}" }
            require(vocabFile.isFile) { "missing ${vocabFile.absolutePath}" }

            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(threads.coerceAtLeast(1))
                setInterOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            val env = OrtEnvironment.getEnvironment()
            val encoder = try {
                env.createSession(encoderFile.absolutePath, options)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Cannot open ${encoderFile.name}", e)
                throw e
            }
            val joint = try {
                env.createSession(jointFile.absolutePath, options)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Cannot open ${jointFile.name}", e)
                encoder.close()
                throw e
            }
            try {
                val vocabulary = vocabFile.inputStream().use { ParakeetVocabulary.parse(it) }
                val encIn = encoder.inputNames.toList()
                val encOut = encoder.outputNames.toList()
                val jointIn = joint.inputNames.toList()
                val jointOut = joint.outputNames.toList()
                return ParakeetRecognizer(
                    encoder = encoder,
                    joint = joint,
                    vocabulary = vocabulary,
                    audioInput = resolve(encIn, "audio_signal"),
                    lengthInput = resolve(encIn, "length"),
                    encoderOutput = resolve(encOut, "outputs", "encoder_outputs"),
                    encoderLengthsOutput = resolve(encOut, "encoded_lengths"),
                    frameInput = resolve(jointIn, "encoder_outputs"),
                    targetsInput = resolve(jointIn, "targets"),
                    targetLengthInput = resolve(jointIn, "target_length"),
                    statesIn1 = resolve(jointIn, "input_states_1"),
                    statesIn2 = resolve(jointIn, "input_states_2"),
                    logitsOutput = resolve(jointOut, "outputs", "logits"),
                    statesOut1 = resolve(jointOut, "output_states_1"),
                    statesOut2 = resolve(jointOut, "output_states_2"),
                )
            } catch (e: Exception) {
                encoder.close()
                joint.close()
                throw e
            }
        }

        private fun resolve(available: List<String>, vararg preferred: String): String {
            for (name in preferred) if (available.contains(name)) return name
            throw IllegalStateException(
                "none of ${preferred.toList()} are among the graph's names $available",
            )
        }
    }
}
