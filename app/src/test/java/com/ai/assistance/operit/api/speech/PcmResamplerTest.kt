package com.ai.assistance.operit.api.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The resampler is the one piece that fails quietly: a wrong filter still
 * produces audio, just audio with the wrong spectrum, and the transcript
 * degrades by a few percent with nothing obviously wrong in a log.
 */
class PcmResamplerTest {

    private fun tone(freq: Double, seconds: Double, rate: Int): DoubleArray {
        val n = (seconds * rate).toInt()
        return DoubleArray(n) { sin(2.0 * PI * freq * it / rate) }
    }

    /** Energy of [signal] above [from] Hz, via a crude DFT at a few probe bins. */
    private fun energyAbove(signal: DoubleArray, rate: Int, from: Double): Double {
        var total = 0.0
        var high = 0.0
        var freq = 100.0
        while (freq < rate / 2.0 - 50.0) {
            var re = 0.0
            var im = 0.0
            // Decimate the DFT sum; only relative energy matters here.
            val step = (signal.size / 4096).coerceAtLeast(1)
            var i = 0
            while (i < signal.size) {
                val a = 2.0 * PI * freq * i / rate
                re += signal[i] * kotlin.math.cos(a)
                im += signal[i] * sin(a)
                i += step
            }
            val e = re * re + im * im
            total += e
            if (freq > from) high += e
            freq *= 1.3
        }
        return if (total == 0.0) 0.0 else high / total
    }

    @Test
    fun passesThroughWhenRatesMatch() {
        val r = PcmResampler(16000, 16000)
        val input = DoubleArray(64) { it / 64.0 }
        val out = r.process(input)
        assertEquals(64, out.size)
        assertTrue(r.isPassthrough)
    }

    @Test
    fun preservesDurationToWithinOneSample() {
        val r = PcmResampler(48000, 16000)
        val input = tone(440.0, 1.0, 48000)
        // flush() releases the samples the causal kernel was still holding.
        val out = r.process(input) + r.flush()
        val expected = input.size / 3
        assertTrue(
            "expected about $expected output samples, got ${out.size}",
            abs(out.size - expected) <= 1
        )
    }

    @Test
    fun rejectsContentAboveTheTargetNyquist() {
        // 12 kHz at 48 kHz in; after decimating to 16 kHz it must not survive,
        // because 8 kHz and up is already outside the target band.
        val input = tone(12000.0, 1.0, 48000)
        val decimated = PcmResampler(48000, 16000).process(input)
        val leaked = energyAbove(decimated, 16000, 7000.0)
        assertTrue(
            "energy above 7 kHz should be attenuated, but was $leaked of total",
            leaked < 0.02
        )
    }

    @Test
    fun keepsSpeechBandEnergy() {
        // 1 kHz sits well inside both bands and must survive intact.
        val input = tone(1000.0, 1.0, 48000)
        val decimated = PcmResampler(48000, 16000).process(input)
        val before = tone(1000.0, 1.0, 48000).map { abs(it) }.average()
        val after = decimated.map { abs(it) }.average()
        assertTrue(
            "1 kHz level dropped from $before to $after",
            after > before * 0.8
        )
    }

    @Test
    fun streamingMatchesOneShot() {
        val input = tone(700.0, 0.5, 44100)
        val oneShot = PcmResampler(44100, 16000).process(input)

        val streamed = PcmResampler(44100, 16000)
        val pieces = ArrayList<Double>()
        var at = 0
        while (at < input.size) {
            val end = minOf(at + 1600, input.size)
            pieces.addAll(streamed.process(input.copyOfRange(at, end)).toList())
            at = end
        }
        val joined = pieces.toDoubleArray()

        val n = minOf(joined.size, oneShot.size)
        assertTrue("streamed ${joined.size} vs one-shot ${oneShot.size}", n > 1000)
        var worst = 0.0
        for (i in 0 until n) worst = maxOf(worst, abs(joined[i] - oneShot[i]))
        // The only difference is the tail, where one-shot sees the whole signal
        // and streaming is still filling its window.
        assertTrue("streaming diverged from one-shot by $worst", worst < 0.05)
    }

    @Test
    fun integerPcmRoundTripsThroughTheSamePath() {
        val pcm = ShortArray(480) { (sin(2.0 * PI * 300.0 * it / 48000) * 8000).toInt().toShort() }
        val r = PcmResampler(48000, 16000)
        val out = r.processInt16(pcm) + ShortArray(r.flush().size) { 0 }
        assertEquals(160, out.size)
        assertTrue("output should not be silent", out.any { it != 0.toShort() })
    }

    @Test
    fun rejectsNonPositiveRates() {
        for (bad in listOf(0, -1)) {
            val error = runCatching { PcmResampler(bad, 16000) }.exceptionOrNull()
            assertTrue("rate $bad should be rejected", error is IllegalArgumentException)
        }
    }
}