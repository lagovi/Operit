package com.ai.assistance.operit.ui.features.chat.components.part

/**
 * Splits a tool result for the detail dialog.
 *
 * Tool results arrive as a single-line JSON envelope
 * (`{"success":..,"message":..,"data":{...}}`), which used to be dumped
 * verbatim. The envelope is still shown in full so no detail is lost, but
 * indented for reading, with the message line lifted above it.
 *
 * @return the message line when the result is a JSON envelope carrying one,
 *   plus the body: the same JSON indented, or the raw text untouched when it
 *   is not JSON at all. Nothing is dropped or summarised, only laid out.
 */
internal fun splitEnvelope(result: String): Pair<String?, String> {
    val trimmed = result.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
        return Pair(null, result)
    }
    return try {
        val json = org.json.JSONObject(trimmed)
        val headline = json.optString("message").takeIf { it.isNotEmpty() }
        Pair(headline, json.toString(2))
    } catch (_: Exception) {
        Pair(null, result)
    }
}
