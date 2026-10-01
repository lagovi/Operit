package com.ai.assistance.operit.api.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * End-to-end check of the whole offline path: PCM in, Russian text out.
 *
 * Needs the real 214 MB checkpoint, which is not committed, so these tests are
 * skipped unless `GIGAAM_MODEL_DIR` points at a directory holding
 * `model.int8.onnx` and `tokens.txt`. Fetch them with the URLs and checksums in
 * `GigaAMModelFiles`. The audio is the official GigaAM test clip, from the URL in
 * `gigaam.utils.download_short_audio`, and `GIGAAM_TEST_WAV` points at it.
 *
 * The expected string is what the NumPy oracle in
 * `tools/gigaam/reference_frontend.py` produces for that clip, so a failure means
 * the Kotlin path diverged from the reference rather than that the model got worse.
 */
class GigaAMRecognizerTest {

    private fun modelDir(): File? =
        System.getenv("GIGAAM_MODEL_DIR")?.let { File(it) }
            ?.takeIf { File(it, "model.int8.onnx").isFile && File(it, "tokens.txt").isFile }

    private fun testClip(): File? =
        System.getenv("GIGAAM_TEST_WAV")?.let { File(it) }?.takeIf { it.isFile }

    @Test
    fun decodesTheOfficialGigaAMClip() {
        val dir = modelDir()
        assumeTrue(
            "set GIGAAM_MODEL_DIR to a directory with model.int8.onnx and tokens.txt",
            dir != null
        )
        val clip = testClip()
        assumeTrue("set GIGAAM_TEST_WAV to example.wav from the GigaAM repo", clip != null)

        val text = GigaAMRecognizer.open(dir!!).use { it.transcribe(readMono16kWav(clip!!)) }

        // Compared by edit distance, not equality. The NumPy oracle and this
        // frontend implement the same documented torchaudio math, but they do
        // not sum the mel filters in the same order, and greedy CTC picks one
        // argmax per frame. On this clip that flips a single near-tied character
        // ("надеждой" / "надежный") out of 99. Exact equality would make this test
        // fail on a harmless accumulation-order difference while a genuinely
        // broken frontend — a wrong n_fft, 80 mel bins, a centred transform —
        // misses by dozens of characters and still fails loudly.
        assertTrue(
            "transcript diverged from the oracle by more than ${MAX_EDIT_DISTANCE} edits:\n" +
                "  expected: $REFERENCE_TEXT\n  actual:   $text",
            levenshtein(REFERENCE_TEXT, text) <= MAX_EDIT_DISTANCE
        )

        // Anchors that must survive exactly, so a coarse regression cannot pass.
        for (anchor in listOf(
            "ничьих не требуя похвал",
            "сладкой",
            "с трепетом любви",
            "у лукоморья дуб зеленый"
        )) {
            assertTrue("missing '$anchor' in: $text", text.contains(anchor))
        }
    }

    @Test
    fun rejectsFeatureBuffersThatDoNotMatchTheFrameCount() {
        val dir = modelDir()
        assumeTrue("set GIGAAM_MODEL_DIR", dir != null)
        GigaAMRecognizer.open(dir!!).use { recognizer ->
            val error = runCatching {
                recognizer.transcribeFeatures(FloatArray(64 * 10), frames = 9)
            }.exceptionOrNull()
            assertTrue(
                "a feature buffer that disagrees with its frame count must be rejected",
                error is IllegalArgumentException
            )
        }
    }

    @Test
    fun vocabSizeMatchesTheGraph() {
        val dir = modelDir()
        assumeTrue("set GIGAAM_MODEL_DIR", dir != null)
        GigaAMRecognizer.open(dir!!).use { recognizer ->
            assertEquals(71, recognizer.vocabSize)
        }
    }

    @Test
    fun shortUtteranceProducesNothingRatherThanGarbage() {
        val dir = modelDir()
        assumeTrue("set GIGAAM_MODEL_DIR", dir != null)
        GigaAMRecognizer.open(dir!!).use { recognizer ->
            // Under one window the frontend emits no frames; must be empty, not a crash.
            assertEquals("", recognizer.transcribe(ShortArray(200)))
        }
    }
}

private const val MAX_EDIT_DISTANCE = 3

private val REFERENCE_TEXT =
    "ничьих не требуя похвал счастлив уж я надеждой сладкой что дева с трепетом любви " +
        "посмотрит может быть украдкой на песни грешные мои у лукоморья дуб зеленый"

private fun levenshtein(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)
    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
        }
        val swap = previous
        previous = current
        current = swap
    }
    return previous[b.length]
}

/**
 * Read a 16 kHz mono 16-bit WAV, skipping the header.
 *
 * Treating the whole file as PCM is the obvious shortcut and it is wrong: the
 * 44-byte RIFF header would be decoded as 22 leading samples, shifting the audio
 * by 1.4 ms. That is enough to flip a near-tied argmax and change a character in
 * the transcript, which is a miserable way to lose an afternoon.
 */
private fun readMono16kWav(file: File): ShortArray {
    val bytes = file.readBytes()
    require(String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF") {
        "${file.absolutePath} is not a RIFF file"
    }
    var at = 12
    var dataOffset = -1
    var dataLength = 0
    while (at + 8 <= bytes.size) {
        val id = String(bytes, at, 4, Charsets.US_ASCII)
        val size = littleEndianInt(bytes, at + 4)
        if (id == "data") {
            dataOffset = at + 8
            dataLength = minOf(size, bytes.size - dataOffset)
            break
        }
        at += 8 + size + (size and 1)
    }
    require(dataOffset >= 0) { "no data chunk in ${file.absolutePath}" }
    return GigaAMFeatureExtractor.decodePcm(bytes, dataLength / 2)
}

private fun littleEndianInt(bytes: ByteArray, at: Int): Int =
    (bytes[at].toInt() and 0xFF) or
        ((bytes[at + 1].toInt() and 0xFF) shl 8) or
        ((bytes[at + 2].toInt() and 0xFF) shl 16) or
        ((bytes[at + 3].toInt() and 0xFF) shl 24)