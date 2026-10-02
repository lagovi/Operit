package com.ai.assistance.operit.api.speech

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generalization must not move the reference path: a default config has
 * to behave bit-identically to the GigaAM object, and anything else only has
 * to be structurally sound, because no oracle exists for numbers we invent.
 */
class CtcModelConfigTest {

    private fun noiseSamples(n: Int): ShortArray {
        var seed = 0x12345678L
        return ShortArray(n) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L)
            ((seed ushr 33) % 20001 - 10000).toInt().toShort()
        }
    }

    @Test
    fun defaultConfigMatchesReferenceConstants() {
        val config = CtcModelConfig.GIGAAM_DEFAULT
        assertEquals(GigaAMFeatureExtractor.SAMPLE_RATE, config.sampleRate)
        assertEquals(GigaAMFeatureExtractor.N_MELS, config.nMels)
        assertEquals(GigaAMFeatureExtractor.WIN_LENGTH, config.winLength)
        assertEquals(GigaAMFeatureExtractor.HOP_LENGTH, config.hopLength)
        assertEquals(GigaAMFeatureExtractor.N_FFT, config.nFft)
        assertEquals(GigaAMFeatureExtractor.CENTER, config.center)
    }

    @Test
    fun defaultFrontendIsBitIdenticalToReference() {
        val samples = noiseSamples(8000)
        val viaObject = GigaAMFeatureExtractor.extract(samples)
        val viaConfig = LogMelFrontend(CtcModelConfig.GIGAAM_DEFAULT).extract(samples)
        assertArrayEquals(viaObject, viaConfig, 0f)
        assertEquals(
            GigaAMFeatureExtractor.frameCount(samples.size),
            LogMelFrontend(CtcModelConfig.GIGAAM_DEFAULT).frameCount(samples.size),
        )
    }

    @Test
    fun otherMelCountChangesOnlyTheFeatureDimension() {
        val config = CtcModelConfig(nMels = 80)
        val samples = noiseSamples(8000)
        val frames = LogMelFrontend(config).frameCount(samples.size)
        assertTrue(frames > 0)
        val feats = LogMelFrontend(config).extract(samples)
        assertEquals(80 * frames, feats.size)
    }

    @Test
    fun otherFramingChangesOnlyTheFrameCount() {
        val config = CtcModelConfig(winLength = 400, hopLength = 160, nFft = 512)
        val samples = noiseSamples(8000)
        val expected = 1 + (8000 - 400) / 160
        assertEquals(expected, LogMelFrontend(config).frameCount(samples.size))
        assertEquals(64 * expected, LogMelFrontend(config).extract(samples).size)
    }

    @Test
    fun shortInputYieldsNoFrames() {
        val config = CtcModelConfig(winLength = 400, hopLength = 160, nFft = 512)
        assertEquals(0, LogMelFrontend(config).frameCount(399))
        assertEquals(0, LogMelFrontend(config).extract(ShortArray(399)).size)
    }

    @Test
    fun frontendIsDeterministic() {
        val frontend = LogMelFrontend(CtcModelConfig(nMels = 80))
        val samples = noiseSamples(4000)
        assertArrayEquals(frontend.extract(samples), frontend.extract(samples), 0f)
    }

    @Test
    fun invalidConfigsFailLoudly() {
        val bad = listOf<() -> CtcModelConfig>(
            { CtcModelConfig(nMels = 0) },
            { CtcModelConfig(winLength = -1) },
            { CtcModelConfig(hopLength = 0) },
            { CtcModelConfig(subsamplingFactor = 0) },
            { CtcModelConfig(blankId = -2) },
            { CtcModelConfig(nFft = 256) },
            { CtcModelConfig(center = true) },
            { CtcModelConfig(sampleRate = 0) },
        )
        for ((index, make) in bad.withIndex()) {
            val error = runCatching { make() }.exceptionOrNull()
            assertTrue("case $index was accepted", error is IllegalArgumentException)
        }
    }

    @Test
    fun blankOverrideIsHonoured() {
        val tokens = "a 0\nb 1\n<blk> 2\n"
        val stream = { ByteArrayInputStream(tokens.toByteArray()) }
        assertEquals(2, GigaAMTokenVocabulary.parse(stream()).blankId)
        assertEquals(0, GigaAMTokenVocabulary.parse(stream(), blankOverride = 0).blankId)
        // Out of range falls back to last id rather than crashing the session.
        assertEquals(2, GigaAMTokenVocabulary.parse(stream(), blankOverride = 9).blankId)
    }
}
