package com.ai.assistance.operit.api.speech

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Resamples mono PCM to the 16 kHz the model expects.
 *
 * GigaAM's preprocessor is fixed at 16 kHz, but that is not a rate `AudioRecord`
 * can be relied on to deliver. The sherpa path this replaces simply asks the
 * microphone for 16 kHz and hopes, which works on most hardware and returns
 * `STATE_UNINITIALIZED` on the rest; capture then falls back to the device rate,
 * usually 48 kHz, and this converts.
 *
 * Linear interpolation is not enough when decimating. At 48 kHz to 16 kHz, energy
 * that sits between 8 and 24 kHz folds back into the speech band and costs
 * accuracy in a way that is almost invisible in a transcript. So the signal is
 * low-passed with a Kaiser-windowed sinc at the lower of the two Nyquist limits
 * before being sampled at the new spacing.
 *
 * The filter is stateful so streaming works: [process] keeps the pre-roll it
 * needs, so a block boundary is indistinguishable from continuous input.
 */
class PcmResampler(val inputRate: Int, val outputRate: Int) {

    private val taps: Int
    private val kernel: DoubleArray
    private val ratio: Double

    /** Samples buffered so far; `bufferStart` is the global index of element 0. */
    private var buffer = DoubleArray(TAPS_PER_SIDE)
    private var bufferStart = -TAPS_PER_SIDE.toLong()

    /** Index of the next output sample to produce. */
    private var nextOutput = 0L

    /** Total real input samples fed so far; bounds output during [flush]. */
    private var inputLength = 0L

    init {
        require(inputRate > 0 && outputRate > 0) { "rates must be positive, got $inputRate and $outputRate" }
        ratio = inputRate.toDouble() / outputRate
        taps = 2 * TAPS_PER_SIDE + 1
        kernel = buildKernel()
    }

    /** True when the input already matches the target. */
    val isPassthrough: Boolean get() = inputRate == outputRate

    /** Global read position of output sample [index]. */
    private fun centreOf(index: Long): Double = index * ratio

    /**
     * Sinc low-pass with a Kaiser window, cutoff at half the lower rate, unit
     * gain so the level matches the microphone.
     */
    private fun buildKernel(): DoubleArray {
        val cutoff = 0.5 * minOf(inputRate, outputRate) / inputRate
        val n = DoubleArray(taps) { i ->
            val k = i - TAPS_PER_SIDE
            val x = k.toDouble() / TAPS_PER_SIDE
            val sinc = if (k == 0) 1.0 else sin(PI * cutoff * x) / (PI * x)
            val inside = (1.0 - x * x).coerceAtLeast(0.0)
            sinc * (besselI0(KITCHEN_SINK_BETA * sqrt(inside)) / besselI0(KITCHEN_SINK_BETA))
        }
        // Unit DC gain: speech level must not change just because we resampled.
        val sum = n.sum()
        if (sum != 0.0) for (i in n.indices) n[i] /= sum
        return n
    }

    /** Convert one block, carrying filter state. Input normalised to [-1, 1]. */
    fun process(input: DoubleArray): DoubleArray {
        if (isPassthrough) return input.copyOf()
        if (input.isEmpty()) return DoubleArray(0)

        val merged = DoubleArray(buffer.size + input.size)
        System.arraycopy(buffer, 0, merged, 0, buffer.size)
        System.arraycopy(input, 0, merged, buffer.size, input.size)
        buffer = merged
        inputLength += input.size

        return produce()
    }

    /** Upper bound on outputs for a block of [inputLength] samples. */
    fun maxOutputFor(inputLength: Int): Int {
        if (isPassthrough) return inputLength
        val available = buffer.size + inputLength
        return (available / ratio).toInt() + 2
    }

    /** Convenience for converting a whole buffer at once. */
    fun resample(input: DoubleArray): DoubleArray = process(input)

    /**
     * Zero-pad the filter and emit whatever is still owed.
     *
     * The kernel is causal, so the last few output samples of a finite stream
     * need input that does not exist yet. Without this the tail is silently
     * dropped, which costs the final phoneme at the end of an utterance. Call
     * once at end of stream, not per block.
     */
    fun flush(): DoubleArray {
        if (isPassthrough) return DoubleArray(0)
        // Two full sides of the kernel is more than enough padding.
        val pad = DoubleArray(taps * 2)
        val merged = DoubleArray(buffer.size + pad.size)
        System.arraycopy(buffer, 0, merged, 0, buffer.size)
        buffer = merged
        return produce()
    }

    private fun produce(): DoubleArray {
        val out = DoubleArray(maxOutputFor(0) + taps)
        var written = 0
        while (true) {
            val centre = centreOf(nextOutput)
            val from = floor(centre - TAPS_PER_SIDE)
            val to = centre + TAPS_PER_SIDE
            // A window centred past the end of the real signal is not a sample
            // of it, however much zero padding flush() added.
            if (centre >= inputLength) break
            if (from < bufferStart.toDouble()) break
            if (to > bufferStart + buffer.size - 1) break

            var acc = 0.0
            var at = from
            for (n in 0 until taps) {
                val local = (at - bufferStart).toInt()
                if (local in buffer.indices) acc += buffer[local] * kernel[n]
                at += 1.0
            }
            out[written++] = acc
            nextOutput++
        }
        val keepFrom = floor(centreOf(nextOutput) - TAPS_PER_SIDE)
        val drop = (keepFrom - bufferStart).toInt().coerceIn(0, buffer.size)
        if (drop > 0) {
            buffer = buffer.copyOfRange(drop, buffer.size)
            bufferStart += drop
        }
        return out.copyOf(written)
    }

    /** Convert int16 PCM, which is what `AudioRecord` delivers. */
    fun processInt16(input: ShortArray): ShortArray {
        val asDouble = DoubleArray(input.size) { input[it] / PCM_SCALE }
        val out = process(asDouble)
        return ShortArray(out.size) { (out[it] * PCM_SCALE).toInt().coerceIn(-32768, 32767).toShort() }
    }

    companion object {
        private const val TAPS_PER_SIDE = 16
        private const val KITCHEN_SINK_BETA = 8.6
        private const val PCM_SCALE = 32768.0

        /** Modified Bessel function of the first kind, order 0, by series. */
        internal fun besselI0(x: Double): Double {
            if (x == 0.0) return 1.0
            val ax = kotlin.math.abs(x)
            if (ax < 3.75) {
                val y = (x / 3.75) * (x / 3.75)
                return 1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 +
                    y * (0.2659732 + y * (0.0360768 + y * 0.0045813)))))
            }
            val y = 3.75 / ax
            return (exp(ax) / sqrt(ax)) *
                (0.39894228 + y * (0.01328592 + y * (0.00225319 + y * (-0.00157565 +
                    y * (0.00916281 + y * (-0.02057706 + y * (0.02635537 +
                    y * (-0.01647633 + y * 0.00392377))))))))
        }
    }
}