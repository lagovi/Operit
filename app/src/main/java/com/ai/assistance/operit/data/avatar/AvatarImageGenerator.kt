package com.ai.assistance.operit.data.avatar

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private val Context.avatarGenerationDataStore by preferencesDataStore(name = "avatar_generation")

/**
 * Text-to-image generation for avatars.
 *
 * The endpoint is explicit rather than inferred from the chat model config: an
 * image endpoint is a different API surface from a chat completion endpoint,
 * and guessing wrong produces confusing failures. Defaults follow the common
 * OpenAI images API shape; Colab, Bonsai and any compatible server can be
 * pointed at with the same fields.
 */
object AvatarImageGenerationSettings {

    private val KEY_ENDPOINT = stringPreferencesKey("endpoint")
    private val KEY_API_KEY = stringPreferencesKey("api_key")
    private val KEY_MODEL = stringPreferencesKey("model")

    data class Snapshot(
        val endpoint: String,
        val apiKey: String,
        val model: String
    )

    fun observe(context: Context) = context.avatarGenerationDataStore.data.map { prefs ->
        Snapshot(
            endpoint = prefs[KEY_ENDPOINT].orEmpty(),
            apiKey = prefs[KEY_API_KEY].orEmpty(),
            model = prefs[KEY_MODEL].orEmpty()
        )
    }

    suspend fun current(context: Context): Snapshot = observe(context).first()

    suspend fun save(
        context: Context,
        endpoint: String,
        apiKey: String,
        model: String
    ) {
        context.avatarGenerationDataStore.edit { prefs ->
            prefs[KEY_ENDPOINT] = endpoint.trim()
            prefs[KEY_API_KEY] = apiKey.trim()
            prefs[KEY_MODEL] = model.trim()
        }
    }
}

object AvatarImageGenerator {

    private const val TAG = "AvatarImageGenerator"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Image generation is not a fast request: a local diffusion model can take
    // a minute or more per image, and OkHttp's 10 second default closes the
    // socket before the server answers. A slow reply is normal here, so the
    // timeouts cover a full generation plus the download of the result.
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .callTimeout(12, TimeUnit.MINUTES)
            .build()
    }

    suspend fun generate(
        context: Context,
        prompt: String,
        size: Int = 512
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = AvatarImageGenerationSettings.current(context)
            require(settings.endpoint.isNotBlank()) {
                "Set the image endpoint in Settings → Avatar generation first"
            }
            val payload = buildString {
                append("{\"prompt\":")
                append(jsonQuote(prompt))
                append(",\"n\":1,\"size\":\"")
                append(size)
                append("x")
                append(size)
                append('"')
                if (settings.model.isNotBlank()) {
                    append(",\"model\":")
                    append(jsonQuote(settings.model))
                }
                append(",\"response_format\":\"b64_json\"}")
            }
            val requestBuilder = Request.Builder()
                .url(settings.endpoint)
                .post(payload.toRequestBody("application/json".toMediaType()))
            if (settings.apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${settings.apiKey}")
            }
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Image endpoint returned HTTP ${response.code}: ${body.take(300)}")
                }
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?: error("Image endpoint returned no data array")
                val base64 = data["b64_json"]?.jsonPrimitive?.content
                val url = data["url"]?.jsonPrimitive?.content
                val uri = when {
                    !base64.isNullOrBlank() ->
                        AvatarAssetStore.saveBase64(context, base64, "generated_avatar")
                    !url.isNullOrBlank() -> downloadToAvatar(context, url)
                    else -> null
                }
                requireNotNull(uri) { "Image endpoint returned neither b64_json nor url" }
                uri
            }
        }.onFailure {
            AppLogger.w(TAG, "Avatar generation failed", it)
        }
    }

    private suspend fun downloadToAvatar(context: Context, url: String): Uri? {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Generated image download failed: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: return null
            return AvatarAssetStore.saveBytes(context, bytes, "generated_avatar")
        }
    }

    private fun jsonQuote(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
        append('"')
    }
}
