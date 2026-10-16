package com.ai.assistance.operit.api.speech

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Log-mel frontend matching onnx-asr `nemo128` (NeMo preprocessor).
 *
 * Every number comes from the onnx-asr reference (`preprocessors/nemo.py`
 * + `preprocessors/fbanks.py`), not from convention:
 * preemphasis 0.97 (first sample kept), 256 zero-pad each side, 512-sample
 * frames every 160, symmetric Hann-400 zero-padded to 512, |rfft|^2,
 * 128-bin Slaney mel bank (Slaney norm), `ln(x + 2^-24)`, per-utterance
 * mean/variance norm over valid frames (variance over `len - 1`, eps 1e-5,
 * invalid frames zeroed).
 *
 * Output is mel-major [N_MELS, frames]; `validFrames` counts the frames the
 * encoder must read (`n // HOP`, the last produced frame is padding).
 */
object ParakeetFrontend {

    const val SAMPLE_RATE = 16000
    const val N_MELS = 128
    const val N_FFT = 512
    const val WIN_LENGTH = 400
    const val HOP_LENGTH = 160
    const val PAD = N_FFT / 2

    private const val PREEMPH = 0.97
    private const val LOG_GUARD = 5.960464477539063e-8 // 2^-24
    private const val NORM_EPS = 1e-5
    private const val PCM_SCALE = 32768.0

    data class Features(val melMajor: FloatArray, val frames: Int, val validFrames: Int)

    private val melBank: FloatArray by lazy { buildSlaneyBank() }
    private val window: DoubleArray by lazy {
        val hann = DoubleArray(WIN_LENGTH) { i ->
            0.5 - 0.5 * cos(2.0 * Math.PI * i / (WIN_LENGTH - 1))
        }
        DoubleArray(N_FFT).also { w ->
            System.arraycopy(hann, 0, w, (N_FFT - WIN_LENGTH) / 2, WIN_LENGTH)
        }
    }

    fun frameCount(sampleCount: Int): Int = sampleCount / HOP_LENGTH + 1

    fun validFrames(sampleCount: Int): Int = sampleCount / HOP_LENGTH

    fun extract(samples: ShortArray): Features {
        val n = samples.size
        val frames = frameCount(n)
        val valid = validFrames(n)

        val x = DoubleArray(n) { samples[it].toDouble() / PCM_SCALE }
        val y = DoubleArray(n)
        if (n > 0) {
            y[0] = x[0]
            for (i in 1 until n) y[i] = x[i] - PREEMPH * x[i - 1]
        }
        val padded = DoubleArray(n + 2 * PAD).also {
            System.arraycopy(y, 0, it, PAD, n)
        }

        val fft = Radix2Fft(N_FFT)
        val frame = DoubleArray(N_FFT)
        val win = window
        val bank = melBank
        val nFreqs = N_FFT / 2 + 1
        val re = DoubleArray(nFreqs)
        val im = DoubleArray(nFreqs)
        val logMel = Array(frames) { FloatArray(N_MELS) }
        for (f in 0 until frames) {
            val base = f * HOP_LENGTH
            for (i in 0 until N_FFT) {
                frame[i] = if (base + i < padded.size) padded[base + i] * win[i] else 0.0
            }
            fft.powerSpectrum(frame, re, im)
            for (m in 0 until N_MELS) {
                var acc = 0.0
                val row = m * nFreqs
                for (k in 0 until nFreqs) {
                    val w = bank[row + k]
                    if (w != 0f) acc += w * (re[k] * re[k] + im[k] * im[k])
                }
                logMel[f][m] = ln(acc + LOG_GUARD).toFloat()
            }
        }

        if (valid > 0) {
            // Per-bin mean/variance over valid frames only, exactly like the
            // reference; invalid (padding) frames read back as zeros.
            val out = FloatArray(N_MELS * frames)
            for (m in 0 until N_MELS) {
                var bSum = 0.0
                for (f in 0 until valid) bSum += logMel[f][m]
                val bMean = bSum / valid
                var bVar = 0.0
                for (f in 0 until valid) {
                    val d = logMel[f][m] - bMean
                    bVar += d * d
                }
                val denom = sqrt(bVar / (valid - 1).coerceAtLeast(1) + NORM_EPS)
                for (f in 0 until frames) {
                    out[m * frames + f] = if (f < valid) {
                        ((logMel[f][m] - bMean) / denom).toFloat()
                    } else {
                        0f
                    }
                }
            }
            return Features(out, frames, valid)
        }
        return Features(FloatArray(N_MELS * frames), frames, 0)
    }

    /**
     * Slaney mel bank with Slaney area norm, exactly
     * `melscale_fbanks(n_fft//2+1, 0, sr//2, 128, sr, "slaney", "slaney")`.
     */
    private fun buildSlaneyBank(): FloatArray {
        val nFreqs = N_FFT / 2 + 1
        val allFreqs = DoubleArray(nFreqs) { it * (SAMPLE_RATE / 2.0) / (nFreqs - 1) }
        val mMin = hzToSlaney(0.0)
        val mMax = hzToSlaney(SAMPLE_RATE / 2.0)
        val mPts = DoubleArray(N_MELS + 2) { mMin + (mMax - mMin) * it / (N_MELS + 1) }
        val hzPts = DoubleArray(N_MELS + 2) { slaneyToHz(mPts[it]) }
        val bank = FloatArray(N_MELS * nFreqs)
        for (m in 0 until N_MELS) {
            val left = hzPts[m]
            val centre = hzPts[m + 1]
            val right = hzPts[m + 2]
            val row = m * nFreqs
            for (k in 0 until nFreqs) {
                val f = allFreqs[k]
                val rising = (f - left) / (centre - left)
                val falling = (right - f) / (right - centre)
                var w = minOf(rising, falling)
                if (w < 0.0) w = 0.0
                bank[row + k] = w.toFloat()
            }
            val norm = 2.0 / (right - left)
            for (k in 0 until nFreqs) bank[row + k] = (bank[row + k] * norm).toFloat()
        }
        return bank
    }

    private fun hzToSlaney(freq: Double): Double {
        val eps = Float.MIN_VALUE.toDouble()
        return if (freq < 1000.0) {
            3.0 * freq / 200.0
        } else {
            15.0 + 27.0 * ln(freq / 1000.0 + eps) / ln(6.4)
        }
    }

    private fun slaneyToHz(mel: Double): Double =
        if (mel < 15.0) {
            200.0 * mel / 3.0
        } else {
            1000.0 * Math.pow(6.4, (mel - 15.0) / 27.0)
        }

    /** In-place radix-2 FFT; [N_FFT] is a power of two so no Bluestein needed. */
    private class Radix2Fft(private val n: Int) {
        private val cosTable = DoubleArray(n) { cos(-2.0 * Math.PI * it / n) }
        private val sinTable = DoubleArray(n) { sin(-2.0 * Math.PI * it / n) }
        private val workRe = DoubleArray(n)
        private val workIm = DoubleArray(n)

        fun powerSpectrum(input: DoubleArray, outRe: DoubleArray, outIm: DoubleArray) {
            System.arraycopy(input, 0, workRe, 0, n)
            java.util.Arrays.fill(workIm, 0.0)
            transform(workRe, workIm)
            val half = n / 2
            for (k in 0..half) {
                outRe[k] = workRe[k]
                outIm[k] = workIm[k]
            }
        }

        private fun transform(re: DoubleArray, im: DoubleArray) {
            var j = 0
            for (i in 1 until n) {
                var bit = n shr 1
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
            while (len <= n) {
                val half = len shr 1
                val stride = n / len
                var base = 0
                while (base < n) {
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
    }
}
