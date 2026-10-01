package com.ai.assistance.operit.api.speech

/**
 * Turns a stream of VAD verdicts into utterance chunks for an offline model.
 *
 * The VAD ([OnnxSileroVad.isSpeech]) already hides its own hysteresis: it
 * reports speech only after sustained voicing and keeps reporting it through
 * trailing silence. This class decides what to *keep* around those verdicts:
 * everything since the last commit while speech is ongoing, a short preroll
 * of audio before the onset so the first phoneme survives, and nothing at all
 * while idle, so an hour of silence costs half a second of RAM.
 *
 * Pure logic, no microphone and no threads, so the whole state machine is
 * covered by JVM tests instead of by hope and a device.
 */
internal class VadUtteranceChunker(
    private val prerollSamples: Int = PREROLL_SAMPLES,
    private val minUtteranceSamples: Int = MIN_UTTERANCE_SAMPLES,
    private val maxUtteranceSamples: Int = MAX_UTTERANCE_SAMPLES,
) {
    private val pending = UtteranceBuffer()

    /** Voiced samples since the last emitted chunk; the blip gate counts this. */
    private var voicedSamples = 0

    /** Whether the last fed frame was inside speech. */
    var inSpeech: Boolean = false
        private set

    /**
     * Feed one VAD-sized frame and its verdict.
     *
     * @return a finished utterance when this frame ends one, null otherwise.
     *   Returned audio runs from the onset preroll through the trailing
     *   silence the VAD already waited out, which is the right context for the
     *   decoder on both sides.
     */
    fun feed(frame: ShortArray, speech: Boolean): ShortArray? {
        pending.append(frame)
        if (speech) {
            inSpeech = true
            voicedSamples += frame.size
            if (pending.size >= maxUtteranceSamples) {
                // One unpaused monologue must not grow without bound; the VAD
                // still reports speech, so the next frames start a new chunk.
                val forced = pending.snapshot()
                val voiced = voicedSamples
                pending.clear()
                voicedSamples = 0
                return finished(forced, voiced)
            }
            return null
        }
        if (inSpeech) {
            inSpeech = false
            val done = pending.snapshot()
            val voiced = voicedSamples
            pending.clear()
            voicedSamples = 0
            return finished(done, voiced)
        }
        // Idle: hold only the preroll.
        if (pending.size > prerollSamples) {
            pending.dropFront(pending.size - prerollSamples)
        }
        return null
    }

    /** Append samples that were never VAD-walked, such as a resampler tail. */
    fun appendRaw(samples: ShortArray) {
        pending.append(samples)
    }

    /**
     * End of stream. A half-spoken tail that never reached an endpoint is
     * returned; idle preroll silence is dropped, not decoded.
     */
    fun flush(): ShortArray? {
        if (!inSpeech) {
            pending.clear()
            return null
        }
        inSpeech = false
        val tail = pending.snapshot()
        val voiced = voicedSamples
        pending.clear()
        voicedSamples = 0
        return finished(tail, voiced)
    }

    /**
     * The gate counts voiced audio, not buffer length: a 32 ms blip inside
     * half a second of preroll is still a blip, and decoding it would only
     * print junk into the transcript.
     */
    private fun finished(samples: ShortArray, voiced: Int): ShortArray? =
        if (voiced < minUtteranceSamples) null else samples

    companion object {
        const val PREROLL_SAMPLES = 8000 // 0.5 s at 16 kHz
        const val MIN_UTTERANCE_SAMPLES = 3200 // 0.2 s
        const val MAX_UTTERANCE_SAMPLES = 320_000 // 20 s
    }

    /** Growable 16-bit buffer with front drops, so silence never pins memory. */
    internal class UtteranceBuffer {
        private var data = ShortArray(1 shl 14)
        var size = 0
            private set

        fun append(src: ShortArray, length: Int = src.size) {
            if (size + length > data.size) {
                var capacity = data.size
                while (capacity < size + length) capacity *= 2
                data = data.copyOf(capacity)
            }
            System.arraycopy(src, 0, data, size, length)
            size += length
        }

        fun dropFront(count: Int) {
            val drop = count.coerceIn(0, size)
            if (drop == 0) return
            System.arraycopy(data, drop, data, 0, size - drop)
            size -= drop
        }

        fun snapshot(): ShortArray = data.copyOfRange(0, size)

        fun clear() {
            size = 0
        }
    }
}
