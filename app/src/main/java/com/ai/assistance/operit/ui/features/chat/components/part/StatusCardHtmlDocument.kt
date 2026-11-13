package com.ai.assistance.operit.ui.features.chat.components.part

import android.content.Context
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * The complete <html> document used to render a status card inside a WebView.
 *
 * The icons of <metric> / <badge> are Material Symbols ligature glyphs. The document used to
 * fetch the font from fonts.googleapis.com via <link>: the card text appeared with the document,
 * but the icons had to wait for the whole 5.36 MB variable font to download, taking 10-40 seconds
 * on a weak network, and offline it drew the icon names as literal text (issue #930). The full
 * static font instance is now bundled with the APK and inlined into the document as a data: URI,
 * so icons and text appear together and no network is needed.
 */
internal object StatusCardHtmlDocument {

    /**
     * The full static instance of Google Fonts Material Symbols Rounded v370.
     * The four axes are pinned at opsz=24 / wght=400 / FILL=1 / GRAD=0, matching the rendering
     * style below. The WOFF2 is 456,052 bytes; the same version with the same full icon range as
     * a four-axis variable font is 5,360,840 bytes.
     *
     * Font source:
     * https://fonts.gstatic.com/s/materialsymbolsrounded/v370/syl0-zNym6YjUruM-QrEh7-nyTnjDwKNJ_190FjpZIvDmUSVOK7BDJ_vb9vUSzq3wzLK-P0J-V_Zs-QtQth3-jOcbTCVpeRL2w5rwZu2rIelXxc.woff2
     * Corresponding CSS:
     * https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,1,0&display=block
     * SHA-256: 19a71b30e6267416493a0e4e925a6f14be60a9c3a83148a0dc0dcf507a8fb382
     *
     * Keeping the full font matters: the status card renderer has always accepted any valid
     * Material Symbols name, and historical messages and previously installed tags may contain
     * icons not listed in the current built-in samples.
     */
    const val ICON_FONT_ASSET: String = "fonts/material_symbols_rounded_static.woff2"

    @Volatile
    private var cachedIconFontBase64: String? = null

    /** Each card is an independent WebView; read the font once and cache it, otherwise it would
     *  be read from disk and encoded again for every card. */
    private fun iconFontBase64(context: Context): String {
        cachedIconFontBase64?.let { return it }
        val encoded =
            context.applicationContext.assets.open(ICON_FONT_ASSET).use {
                Base64.encodeToString(it.readBytes(), Base64.NO_WRAP)
            }
        cachedIconFontBase64 = encoded
        return encoded
    }

    fun build(context: Context, bodyContent: String, textColor: Color): String =
        render(
            bodyContent = bodyContent,
            textColorHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb()),
            iconFontBase64 = iconFontBase64(context)
        )

    internal fun render(bodyContent: String, textColorHex: String, iconFontBase64: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    @font-face {
                        font-family: 'Material Symbols Rounded';
                        font-style: normal;
                        font-weight: 400;
                        src: url(data:font/woff2;base64,$iconFontBase64) format('woff2');
                    }
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        font-size: 13px;
                        line-height: 1.4;
                        color: $textColorHex;
                        padding: 0;
                        background: transparent;
                    }
                    .material-symbols-rounded {
                        font-family: 'Material Symbols Rounded';
                        font-weight: normal;
                        font-style: normal;
                        font-size: 20px;
                        display: inline-block;
                        line-height: 1;
                        text-transform: none;
                        letter-spacing: normal;
                        word-wrap: normal;
                        white-space: nowrap;
                        direction: ltr;
                        font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;
                        -webkit-font-feature-settings: 'liga';
                        -webkit-font-smoothing: antialiased;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        margin: 2px 0 3px 0;
                        font-weight: 600;
                        line-height: 1.3;
                        color: inherit;
                    }
                    h1 { font-size: 15px; }
                    h2 { font-size: 14px; }
                    h3 { font-size: 13px; }
                    h4 { font-size: 13px; }
                    h5 { font-size: 12px; }
                    h6 { font-size: 12px; }
                    p {
                        margin: 2px 0;
                        font-size: 13px;
                    }
                    a {
                        color: #007AFF;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    strong, b {
                        font-weight: 600;
                    }
                </style>
            </head>
            <body>
                $bodyContent
            </body>
            </html>
        """.trimIndent()
    }
}
