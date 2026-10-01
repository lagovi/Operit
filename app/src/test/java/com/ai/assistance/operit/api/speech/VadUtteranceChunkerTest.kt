package com.ai.assistance.operit.api.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The chunker is where phrases are born: a wrong boundary here reads as
 * clipped words or duplicated text, and the microphone cannot tell us. So the
 * state machine is scripted frame by frame instead of tested through audio.
 */
class VadUtteranceChunkerTest {

    private val frame = ShortArray(512) { 1 }

    private fun silence(chunker: VadUtteranceChunker, frames: Int): ShortArray? {
        var committed: ShortArray? = null
        repeat(frames) { committed = chunker.feed(frame, false) ?: committed }
        return committed
    }

    private fun speech(chunker: VadUtteranceChunker, frames: Int): ShortArray? {
        var committed: ShortArray? = null
        repeat(frames) { committed = chunker.feed(frame, true) ?: committed }
        return committed
    }

    @Test
    fun silenceCommitsNothingAndHoldsOnlyPreroll() {
        val chunker = VadUtteranceChunker()
        val committed = silence(chunker, 200) // ~6.4 s of quiet
        assertNull(committed)
        assertFalse(chunker.inSpeech)
    }

    @Test
    fun onePhraseCommitsOnceWithPrerollAndTrailingSilence() {
        val chunker = VadUtteranceChunker()
        silence(chunker, 40)
        speech(chunker, 30) // ~1 s of voicing
        // The VAD holds its verdict through trailing silence, then releases.
        repeat(9) { chunker.feed(frame, true) }
        val done = chunker.feed(frame, false)
        assertNotNull(done)
        // Preroll plus speech plus the silence the VAD waited out.
        assertTrue(
            "expected a full phrase, got ${done!!.size} samples",
            done.size >= VadUtteranceChunker.PREROLL_SAMPLES + 30 * 512,
        )
        assertFalse(chunker.inSpeech)
    }

    @Test
    fun singleFrameBlipIsNotSpeech() {
        val chunker = VadUtteranceChunker()
        silence(chunker, 40)
        chunker.feed(frame, true)
        val done = chunker.feed(frame, false)
        assertNull("a 32 ms blip must not reach the decoder, got ${done?.size}", done)
        assertFalse(chunker.inSpeech)
    }

    @Test
    fun longMonologueIsForceCommitted() {
        val chunker = VadUtteranceChunker(
            maxUtteranceSamples = 512 * 10,
        )
        var commits = 0
        repeat(25) {
            if (chunker.feed(frame, true) != null) commits++
        }
        assertTrue("expected force commits, got $commits", commits >= 2)
        assertTrue(chunker.inSpeech)
    }

    @Test
    fun flushReturnsHalfSpokenTail() {
        val chunker = VadUtteranceChunker()
        silence(chunker, 40)
        speech(chunker, 20)
        val tail = chunker.flush()
        assertNotNull(tail)
        assertTrue(tail!!.size >= 20 * 512)
        assertFalse(chunker.inSpeech)
    }

    @Test
    fun flushDropsIdleSilence() {
        val chunker = VadUtteranceChunker()
        silence(chunker, 40)
        assertNull(chunker.flush())
    }

    @Test
    fun rawTailAppendsToSpokenAudio() {
        val chunker = VadUtteranceChunker()
        silence(chunker, 40)
        speech(chunker, 20)
        chunker.appendRaw(ShortArray(100) { 2 })
        val tail = chunker.flush()
        assertNotNull(tail)
        assertEquals(
            VadUtteranceChunker.PREROLL_SAMPLES + 20 * 512 + 100,
            tail!!.size,
        )
    }

    @Test
    fun consecutivePhrasesCommitSeparately() {
        val chunker = VadUtteranceChunker()
        val commits = ArrayList<ShortArray>()
        silence(chunker, 40)
        speech(chunker, 20)
        repeat(9) { chunker.feed(frame, true) }
        chunker.feed(frame, false)?.let { commits.add(it) }
        silence(chunker, 20)
        speech(chunker, 20)
        repeat(9) { chunker.feed(frame, true) }
        chunker.feed(frame, false)?.let { commits.add(it) }
        assertEquals(2, commits.size)
    }
}
