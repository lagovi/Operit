package com.ai.assistance.operit.ui.features.chat.components.part

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolResultFormatTest {

    @Test
    fun envelopeYieldsHeadlineAndIndentedBody() {
        val (headline, body) = splitEnvelope(
            """{"success":true,"message":"done","data":{"level":31}}"""
        )
        assertEquals("done", headline)
        assertTrue(body.contains("\n"))
        assertTrue(body.contains("\"level\": 31"))
    }

    @Test
    fun envelopeWithoutMessageHasNoHeadline() {
        val (headline, body) = splitEnvelope("""{"success":true}""")
        assertNull(headline)
        assertTrue(body.contains("success"))
    }

    @Test
    fun plainTextPassesThroughUntouched() {
        val raw = "just some text"
        val (headline, body) = splitEnvelope(raw)
        assertNull(headline)
        assertEquals(raw, body)
    }

    @Test
    fun brokenJsonPassesThroughUntouched() {
        val raw = """{"success":true,"oops"""
        val (headline, body) = splitEnvelope(raw)
        assertNull(headline)
        assertEquals(raw, body)
    }
}
