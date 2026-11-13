package com.ai.assistance.operit.ui.features.chat.components.part

import android.content.Context
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * The complete <html> document used to render a status card inside a WebView.
 *
 * The icons of <metric> / <badge> are Material Symbols ligature glyphs. The document used
 * to fetch the font from fonts.googleapis.com via <link>: the card text appeared with the
 * document, but the icons had to wait for the whole 5.35 MB variable font to download, taking
 * 10-40 seconds on a weak network, and offline it simply drew the icon names as literal text
 * (issue #930). The font subset is now bundled with the APK and inlined into the document as
 * a data: URI, so icons and text appear together and no network is needed.
 */
internal object StatusCardHtmlDocument {

    /** The icon font bundled in assets, covering only the icons in [ICON_NAMES]. */
    const val ICON_FONT_ASSET: String = "fonts/material_symbols_rounded_subset.woff2"

    /**
     * The icon names the font subset covers, the same list given to the model in the tag
     * market's "AI status card" prompt.
     *
     * Adding or removing an icon requires changing this, that prompt, and regenerating the font
     * with the same set of names (the list stays sorted and deduplicated so the URL below is
     * reproducible):
     * https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,1,0&icon_names=<comma-separated names>
     *
     * All four axes are pinned to single values (opsz=24 / wght=400 / FILL=1 / GRAD=0), so the
     * fetched result is a static instance that matches the font-variation-settings of
     * .material-symbols-rounded below; for these same 140 icons, the variable font with all axis
     * interpolation data is 9 times larger.
     */
    val ICON_NAMES: List<String> = listOf(
        "account_circle", "alarm", "analytics", "auto_awesome", "autorenew",
        "badge", "bar_chart", "battery_alert", "battery_charging_full", "battery_full",
        "bedtime", "block", "bolt", "book", "bookmark",
        "bug_report", "build", "calendar_month", "campaign", "cancel",
        "celebration", "chat", "check_circle", "close", "cloud",
        "cloud_done", "cloud_off", "code", "construction", "dark_mode",
        "data_usage", "description", "developer_board", "diamond", "directions_run",
        "done_all", "draw", "eco", "edit", "electric_bolt",
        "emoji_emotions", "emoji_events", "error", "event", "explore",
        "extension", "face", "favorite", "flight", "forum",
        "grade", "group", "groups", "handshake", "help",
        "history", "home", "hourglass_empty", "hourglass_top", "hub",
        "image", "info", "insights", "key", "leaderboard",
        "light_mode", "lightbulb", "local_cafe", "local_fire_department", "lock",
        "map", "memory", "menu_book", "military_tech", "monitor_heart",
        "monitoring", "mood", "mood_bad", "music_note", "neurology",
        "notifications", "palette", "park", "pause", "pending",
        "person", "person_search", "pets", "pie_chart", "place",
        "play_arrow", "priority_high", "psychology", "psychology_alt", "public",
        "query_stats", "refresh", "report", "restaurant", "rocket_launch",
        "schedule", "science", "search", "security", "self_improvement",
        "sentiment_dissatisfied", "sentiment_neutral", "sentiment_satisfied", "sentiment_very_dissatisfied", "sentiment_very_satisfied",
        "settings", "shield", "show_chart", "smart_toy", "speed",
        "star", "storage", "sync", "task_alt", "terminal",
        "thermostat", "thumb_down", "thumb_up", "timeline", "timer",
        "tips_and_updates", "today", "translate", "travel_explore", "trending_down",
        "trending_up", "tune", "update", "verified", "visibility",
        "water_drop", "waving_hand", "wifi", "wifi_off", "workspace_premium",
    )

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
