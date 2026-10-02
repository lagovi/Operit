package com.ai.assistance.operit.data.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Link parsing and Hub listing interpretation. The network call itself is
 * not covered here — no recorded HTTP in unit tests — but everything around
 * it is: the link shapes users actually paste, and the sibling picking rules
 * that decide what gets downloaded.
 */
class HfModelResolverTest {

    private val resolver = HfModelResolver()

    @Test
    fun bareRepoIdResolvesToMain() {
        val parsed = resolver.parseLink("i2z1/gigaam-multilingual-ctc-onnx-int8").getOrThrow()
        assertEquals("i2z1/gigaam-multilingual-ctc-onnx-int8", parsed.repoId)
        assertNull(parsed.revision)
        assertNull(parsed.preferredPath)
    }

    @Test
    fun repoPageLinkResolves() {
        val parsed = resolver.parseLink(
            "https://huggingface.co/i2z1/gigaam-multilingual-ctc-onnx-int8"
        ).getOrThrow()
        assertEquals("i2z1/gigaam-multilingual-ctc-onnx-int8", parsed.repoId)
        assertNull(parsed.preferredPath)
    }

    @Test
    fun blobLinkPinsRevisionAndFile() {
        val parsed = resolver.parseLink(
            "https://huggingface.co/owner/repo/blob/abc123/model.int8.onnx"
        ).getOrThrow()
        assertEquals("owner/repo", parsed.repoId)
        assertEquals("abc123", parsed.revision)
        assertEquals("model.int8.onnx", parsed.preferredPath)
    }

    @Test
    fun nestedBlobPathIsKeptWhole() {
        val parsed = resolver.parseLink(
            "https://huggingface.co/owner/repo/resolve/main/onnx/model.int8.onnx"
        ).getOrThrow()
        assertEquals("onnx/model.int8.onnx", parsed.preferredPath)
    }

    @Test
    fun garbageLinksFailLoudly() {
        for (bad in listOf("", "   ", "single", "huggingface.co/", "/a", "a/b/../c")) {
            assertTrue(
                "accepted '$bad'",
                resolver.parseLink(bad).isFailure,
            )
        }
    }

    @Test
    fun trailingPathWithoutBlobMarkerIsIgnored() {
        // A tree page or anything else after owner/repo pins nothing.
        val parsed = resolver.parseLink("https://huggingface.co/o/r/tree/main").getOrThrow()
        assertEquals("o/r", parsed.repoId)
        assertNull(parsed.revision)
        assertNull(parsed.preferredPath)
    }

    @Test
    fun singleModelRepoPicksThePair() {
        val body = hubResponse(
            file("model.int8.onnx", size = 1000, oid = "aa"),
            file("tokens.txt", size = -1, oid = null),
            file("README.md", size = -1, oid = null),
        )
        val siblings = resolver.parseSiblings("o/r", "main", body)
        assertEquals(3, siblings.size)
        val model = siblings.first { it.path == "model.int8.onnx" }
        assertEquals(1000L, model.sizeBytes)
        assertEquals("aa", model.sha256)
        assertEquals(
            "https://huggingface.co/o/r/resolve/main/model.int8.onnx",
            model.downloadUrl,
        )
        val tokens = siblings.first { it.path == "tokens.txt" }
        assertEquals("", tokens.sha256)
    }

    @Test
    fun emptyHubResponseYieldsNoSiblings() {
        assertTrue(resolver.parseSiblings("o/r", "main", "{}").isEmpty())
        assertTrue(resolver.parseSiblings("o/r", "main", "not json{").isEmpty())
    }

    private fun file(path: String, size: Long, oid: String?): String {
        val lfs = if (oid != null) {
            """, "lfs": {"oid": "$oid", "size": $size}"""
        } else {
            ""
        }
        return """{"rfilename": "$path"$lfs}"""
    }

    private fun hubResponse(vararg files: String): String =
        """{"siblings": [${files.joinToString(",")}]}"""
}
