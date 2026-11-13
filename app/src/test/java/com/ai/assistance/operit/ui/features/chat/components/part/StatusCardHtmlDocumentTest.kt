package com.ai.assistance.operit.ui.features.chat.components.part

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test for issue #930: the status card's icon font must be inlined into the
 * document, and the document must no longer fetch anything over the network to render icons.
 */
class StatusCardHtmlDocumentTest {

    @Test
    fun `document inlines the bundled icon font`() {
        val html = render()
        assertTrue(html.contains("@font-face"))
        assertTrue(
            html.contains("src: url(data:font/woff2;base64,$FAKE_FONT) format('woff2')")
        )
    }

    @Test
    fun `document requests nothing over the network`() {
        // This used to <link> to fonts.googleapis.com, and the icons stayed empty until the
        // 5.35 MB variable font finished downloading.
        val html = render()
        assertFalse(html.contains("http://"))
        assertFalse(html.contains("https://"))
    }

    @Test
    fun `icon spans still resolve ligatures without the remote stylesheet`() {
        // liga used to come for free with Google's stylesheet; after removing <link> it must be
        // declared here, or <span>favorite</span> is drawn as letters instead of an icon.
        val html = render()
        assertTrue(html.contains(".material-symbols-rounded"))
        assertTrue(html.contains("font-family: 'Material Symbols Rounded'"))
        assertTrue(html.contains("-webkit-font-feature-settings: 'liga'"))
    }

    @Test
    fun `document still carries the card body and text color`() {
        val html = render("<div class=\"metric\">mood</div>")
        assertTrue(html.contains("<div class=\"metric\">mood</div>"))
        assertTrue(html.contains("color: #123456;"))
    }

    @Test
    fun `full static font preserves historical icons outside the former subset`() {
        val html = render(SHOPPING_CART_SPAN)
        assertTrue(html.contains(SHOPPING_CART_SPAN))

        // This Google Fonts v370 static instance has 4,277 GSUB ligatures per a fontTools
        // check, including shopping_cart which was not in the former 140-icon subset. Pinning
        // the file prevents an accidental switch back to the subset later.
        val font = File("src/main/assets/${StatusCardHtmlDocument.ICON_FONT_ASSET}")
        assertTrue(font.isFile)
        assertEquals(FULL_STATIC_FONT_BYTES, font.length())
        assertEquals(FULL_STATIC_FONT_SHA256, sha256(font))
    }

    private fun render(body: String = ICON_SPAN): String =
        StatusCardHtmlDocument.render(
            bodyContent = body,
            textColorHex = "#123456",
            iconFontBase64 = FAKE_FONT
        )

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val FAKE_FONT = "d09GMgABAAAAAA"
        const val ICON_SPAN = "<span class=\"material-symbols-rounded\">favorite</span>"
        const val SHOPPING_CART_SPAN =
            "<span class=\"material-symbols-rounded\">shopping_cart</span>"
        const val FULL_STATIC_FONT_BYTES = 456_052L
        const val FULL_STATIC_FONT_SHA256 =
            "19a71b30e6267416493a0e4e925a6f14be60a9c3a83148a0dc0dcf507a8fb382"
    }
}
