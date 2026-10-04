package com.ai.assistance.operit.data.translation

import com.ai.assistance.operit.util.AppLogger
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Free Google translation endpoint as the user-approved fallback.
 *
 * The primary path is the default LLM ([CachedTranslator] fetch). When it
 * is missing or unreachable the runtime text would stay Chinese, so with
 * the user's explicit approval one step down tries the keyless gtx
 * endpoint. It is unofficial and rate-limited, hence fallback-only: it
 * never runs when the model answers. When both fail the caller shows the
 * source, same as before.
 */
object GoogleTranslateFallback {
    const val TARGET_ENGLISH = "en"

    fun requestUrl(text: String, targetLang: String = TARGET_ENGLISH): String {
        val query = URLEncoder.encode(text, Charsets.UTF_8.name())
        return "https://translate.googleapis.com/translate_a/single" +
            "?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$query"
    }

    /**
     * Parses the gtx sentence array (`[[["Hello", ...], ...], ...]`) into
     * the concatenated translated sentences. Null on any shape mismatch:
     * the caller shows the source instead of a half-parsed guess.
     */
    fun parseResponse(json: String): String? {
        try {
            val sentences = org.json.JSONArray(json).optJSONArray(0) ?: return null
            val out = StringBuilder()
            for (i in 0 until sentences.length()) {
                sentences.optJSONArray(i)?.optString(0)?.let { out.append(it) }
            }
            return out.toString().trim().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cannot parse Google translation response", e)
            return null
        }
    }

    suspend fun translate(
        client: OkHttpClient,
        text: String,
        targetLang: String = TARGET_ENGLISH,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(requestUrl(text, targetLang)).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                parseResponse(response.body?.string().orEmpty())
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Google translation fallback failed", e)
            null
        }
    }

    private const val TAG = "GoogleTranslateFallback"
}
