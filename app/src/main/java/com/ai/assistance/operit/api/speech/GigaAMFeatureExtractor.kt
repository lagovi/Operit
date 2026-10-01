package com.ai.assistance.operit.api.speech

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Log-mel spectrogram frontend for GigaAM-Multilingual CTC.
 *
 * Every constant comes from the model's own `multilingual_ctc.yaml`, not from
 * convention. Two are easy to get wrong:
 *
 *  - [N_MELS] is 64. The sherpa-onnx examples for this model print
 *    `featureDim: 80`, which is a placeholder from their generic nemo_ctc
 *    sample and is wrong for this checkpoint.
 *  - [CENTER] is false, so there is no reflect padding and the first output
 *    frame covers samples 0..[WIN_LENGTH]. The frame count follows
 *    `(n - [WIN_LENGTH]) / [HOP_LENGTH] + 1`.
 *
 * The spectrum is a power spectrogram (`magnitude ** 2`), the mel filters use
 * the HTK scale with no Slaney area normalisation — both are
 * `torchaudio.transforms.MelSpectrogram` at its defaults — and the result is
 * `ln(clamp(x, 1e-9, 1e9))`, which is `gigaam.preprocess.SpecScaler`.
 *
 * The array handed to the recogniser is [N_MELS] x frameCount, mel-major,
 * because the exported graph declares its input `[batch, 64, seq_len]`.
 * Feeding `[batch, seq_len, 64]` does not fail; it silently returns noise.
 *
 * `tools/gigaam/reference_frontend.py` is the oracle for this file, and
 * `GigaAMFeatureExtractorTest` asserts the two agree.
 */
object GigaAMFeatureExtractor {

    const val SAMPLE_RATE = 16000
    const val N_MELS = 64
    const val WIN_LENGTH = 320
    const val HOP_LENGTH = 160
    const val N_FFT = 320
    const val CENTER = false

    private const val LOG_CLAMP_MIN = 1e-9
    private const val LOG_CLAMP_MAX = 1e9

    /** gigaam.preprocess.load_audio scales int16 by this to reach [-1, 1]. */
    private const val PCM_SCALE = 32768.0

    private const val MEL_HZ_FACTOR = 2595.0
    private const val MEL_HZ_SCALE = 700.0

    private val filterbank: DoubleArray by lazy { buildMelFilterbank() }
    private val window: FloatArray by lazy { buildHannWindow() }
    private val plan: DftPlan by lazy { DftPlan(N_FFT) }

    /** Output frames for [sampleCount] input samples, or 0 when it is too short. */
    fun frameCount(sampleCount: Int): Int {
        if (sampleCount < WIN_LENGTH) return 0
        return 1 + (sampleCount - WIN_LENGTH) / HOP_LENGTH
    }

    /**
     * @param samples mono PCM, treated as int16 and divided by [PCM_SCALE] to
     *   reach [-1, 1], matching load_audio.
     * @return [N_MELS] * `frameCount(samples.size)` float32 values, mel-major.
     */
    fun extract(samples: ShortArray): FloatArray {
        val frames = frameCount(samples.size)
        val out = FloatArray(N_MELS * frames)
        if (frames == 0) return out

        val win = window
        val fb = filterbank
        val nFreqs = N_FFT / 2 + 1

        // Allocated once and reused for every frame.
        val windowed = DoubleArray(N_FFT)
        // Double, not Float: the oracle sums the mel filters in float64, and
        // rounding the power spectrum to float32 first flips near-tied argmax
        // decisions — enough to change a character in the decoded text.
        val power = DoubleArray(nFreqs)
        val melAcc = FloatArray(N_MELS)
        val binReD = DoubleArray(nFreqs)
        val binImD = DoubleArray(nFreqs)

        for (f in 0 until frames) {
            val base = f * HOP_LENGTH

            // Window the frame, zero padding anything past the signal end.
            java.util.Arrays.fill(windowed, 0.0)
            val limit = min(WIN_LENGTH, samples.size - base)
            for (i in 0 until limit) {
                windowed[i] = (samples[base + i].toDouble() / PCM_SCALE) * win[i]
            }

            plan.transform(windowed, binReD, binImD)
            for (k in 0 until nFreqs) {
                val r = binReD[k]
                val i2 = binImD[k]
                power[k] = r * r + i2 * i2
            }

            for (m in 0 until N_MELS) {
                val row = m * nFreqs
                var acc = 0.0
                for (k in 0 until nFreqs) {
                    val w = fb[row + k]
                    if (w != 0.0) acc += w * power[k]
                }
                melAcc[m] = ln(max(min(acc, LOG_CLAMP_MAX), LOG_CLAMP_MIN)).toFloat()
            }
            for (m in 0 until N_MELS) {
                out[m * frames + f] = melAcc[m]
            }
        }
        return out
    }

    /** Decode little-endian int16 PCM, the layout AudioRecord produces. */
    fun decodePcm(bytes: ByteArray, count: Int): ShortArray {
        val buffer = ByteBuffer.wrap(bytes, 0, count * 2).order(ByteOrder.LITTLE_ENDIAN)
        val out = ShortArray(count)
        for (i in 0 until count) out[i] = buffer.short
        return out
    }

    /**
     * HTK mel scale, triangular filters over the rfft bins, no area
     * normalisation. Matches torchaudio's `mel_scale="htk"`, `norm=None`.
     */
    private fun buildMelFilterbank(): DoubleArray {
        val nFreqs = N_FFT / 2 + 1
        val filters = DoubleArray(N_MELS * nFreqs)

        val fftFreqs = DoubleArray(nFreqs) { it * (SAMPLE_RATE / 2.0) / (nFreqs - 1) }
        val melMin = hzToMel(0.0)
        val melMax = hzToMel(SAMPLE_RATE / 2.0)
        val melPoints = DoubleArray(N_MELS + 2) { melMin + (melMax - melMin) * it / (N_MELS + 1) }
        val hzPoints = DoubleArray(N_MELS + 2) { melToHz(melPoints[it]) }

        for (m in 0 until N_MELS) {
            val left = hzPoints[m]
            val centre = hzPoints[m + 1]
            val right = hzPoints[m + 2]
            val row = m * nFreqs
            if (right <= left) {
                var nearest = 0
                var best = Double.MAX_VALUE
                for (k in 0 until nFreqs) {
                    val d = abs(fftFreqs[k] - centre)
                    if (d < best) {
                        best = d
                        nearest = k
                    }
                }
                filters[row + nearest] = 1.0
                continue
            }
            for (k in 0 until nFreqs) {
                val f = fftFreqs[k]
                val rising = (f - left) / (centre - left)
                val falling = (right - f) / (right - centre)
                val w = min(rising, falling)
                if (w > 0.0) filters[row + k] = w
            }
        }
        return filters
    }

    /** `torch.hann_window(win_length, periodic=True)`. */
    private fun buildHannWindow(): FloatArray =
        FloatArray(WIN_LENGTH) { i -> (0.5 - 0.5 * cos(2.0 * PI * i / WIN_LENGTH)).toFloat() }

    private fun hzToMel(hz: Double): Double =
        MEL_HZ_FACTOR * Math.log10(1.0 + hz / MEL_HZ_SCALE)

    private fun melToHz(mel: Double): Double =
        MEL_HZ_SCALE * (Math.pow(10.0, mel / MEL_HZ_FACTOR) - 1.0)

    /**
     * Forward DFT of arbitrary length, computed with Blestein's algorithm.
     *
     * n_fft here is 320 = 2^6 * 5, so it is **not** a power of two and a radix-2
     * Cooley-Tukey pass cannot be used at all: with such a length a stage no
     * longer divides the transform evenly and the inner loop walks past the end
     * of the array. torch.fft.rfft, which torchaudio uses, copes via a general
     * algorithm, and the exported graph expects exactly its bins.
     *
     * Bluestein rewrites the length-n transform as a convolution of the input
     * with a chirp, and the convolution is evaluated with radix-2 transforms of
     * length `m >= 2n - 1`, which is a power of two.
     */
    private class DftPlan(private val n: Int) {
        private val m: Int = run {
            var size = 1
            while (size < 2 * n - 1) size = size shl 1
            size
        }

        /** exp(-i*pi*k^2/n) for k in 0 until n. */
        private val chirpRe = DoubleArray(n)
        private val chirpIm = DoubleArray(n)

        /** FFT of the chirp written backwards, wrapped. */
        private val kernelRe = DoubleArray(m)
        private val kernelIm = DoubleArray(m)

        private val workRe = DoubleArray(m)
        private val workIm = DoubleArray(m)
        private val cosTable = DoubleArray(m) { cos(-2.0 * PI * it / m) }
        private val sinTable = DoubleArray(m) { sin(-2.0 * PI * it / m) }

        init {
            for (k in 0 until n) {
                // k^2 mod 2n keeps the angle in [-pi, pi] without changing e^{i*angle}.
                val j = (k.toLong() * k.toLong()) % (2L * n)
                val angle = -PI * j / n
                chirpRe[k] = cos(angle)
                chirpIm[k] = sin(angle)
            }
            for (k in 0 until n) {
                // b[k] = b[m - k] = conj(chirp[k]) = exp(+i*pi*k^2/n)
                val wr = chirpRe[k]
                val wi = -chirpIm[k]
                kernelRe[k] = wr
                kernelIm[k] = wi
                if (k != 0) {
                    kernelRe[m - k] = wr
                    kernelIm[m - k] = wi
                }
            }
            radix2(kernelRe, kernelIm)
        }

        /**
         * Transforms [input], which must be all reals, and writes bins 0..n/2 into
         * [outRe] and [outIm].
         */
        fun transform(input: DoubleArray, outRe: DoubleArray, outIm: DoubleArray) {
            java.util.Arrays.fill(workRe, 0.0)
            java.util.Arrays.fill(workIm, 0.0)
            for (k in 0 until n) {
                workRe[k] = input[k] * chirpRe[k]
                workIm[k] = input[k] * chirpIm[k]
            }
            radix2(workRe, workIm)

            for (i in 0 until m) {
                val pr = workRe[i]
                val pi = workIm[i]
                workRe[i] = pr * kernelRe[i] - pi * kernelIm[i]
                workIm[i] = pr * kernelIm[i] + pi * kernelRe[i]
            }
            inverseRadix2(workRe, workIm)

            val half = n / 2
            for (k in 0..half) {
                // Multiply by the complex chirp; keeping only its real part
                // silently corrupts both the real and imaginary bins.
                val cr = workRe[k]
                val ci = workIm[k]
                outRe[k] = cr * chirpRe[k] - ci * chirpIm[k]
                outIm[k] = cr * chirpIm[k] + ci * chirpRe[k]
            }
        }

        /** In-place radix-2 Cooley-Tukey. [m] is a power of two. */
        private fun radix2(re: DoubleArray, im: DoubleArray) {
            var j = 0
            for (i in 1 until m) {
                var bit = m shr 1
                while ((j and bit) != 0) {
                    j = j xor bit
                    bit = bit shr 1
                }
                j = j xor bit
                if (i < j) {
                    var t = re[i]; re[i] = re[j]; re[j] = t
                    t = im[i]; im[i] = im[j]; im[j] = t
                }
            }
            var len = 2
            while (len <= m) {
                val half = len shr 1
                val stride = m / len
                var base = 0
                while (base < m) {
                    for (x in 0 until half) {
                        val tw = x * stride
                        val wr = cosTable[tw]
                        val wi = sinTable[tw]
                        val a = base + x
                        val b = a + half
                        val tr = re[b] * wr - im[b] * wi
                        val ti = re[b] * wi + im[b] * wr
                        re[b] = re[a] - tr
                        im[b] = im[a] - ti
                        re[a] += tr
                        im[a] += ti
                    }
                    base += len
                }
                len = len shl 1
            }
        }

        private fun inverseRadix2(re: DoubleArray, im: DoubleArray) {
            for (i in 0 until m) im[i] = -im[i]
            radix2(re, im)
            val scale = 1.0 / m
            for (i in 0 until m) {
                re[i] *= scale
                im[i] = -im[i] * scale
            }
        }
    }
}