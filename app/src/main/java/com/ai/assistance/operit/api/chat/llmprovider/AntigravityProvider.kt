package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.data.api.AntigravityAuthManager
import com.ai.assistance.operit.data.api.AntigravityOAuthProtocol
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelOption
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private class AntigravityAccessTokenProvider(
    private val authManager: AntigravityAuthManager,
) : ApiKeyProvider {
    override suspend fun getApiKey(): String = authManager.getValidAccessToken()

    override suspend fun getCandidateKeyCount(): Int =
        if (authManager.authState.value == null) 0 else 1
}

class AntigravityProvider(
    private val authManager: AntigravityAuthManager,
    apiEndpoint: String,
    modelName: String,
    client: OkHttpClient,
    customHeaders: Map<String, String> = emptyMap(),
    enableToolCall: Boolean = false,
    thinkingConfigurations: String = "",
    thinkingOptionId: String = "",
) : GeminiProvider(
    apiEndpoint = apiEndpoint.ifBlank { AntigravityOAuthProtocol.DEFAULT_ENDPOINT },
    apiKeyProvider = AntigravityAccessTokenProvider(authManager),
    modelName = modelName,
    // Intent: mirror the native client per CLIProxyAPI — HTTP/1.1 with a dedicated short-lived pool.
    client = client.newBuilder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectionPool(ConnectionPool(2, 30, TimeUnit.SECONDS))
        .build(),
    customHeaders = customHeaders,
    providerType = ApiProviderType.ANTIGRAVITY,
    enableGoogleSearch = false,
    enableToolCall = enableToolCall,
    thinkingConfigurations = thinkingConfigurations,
    thinkingOptionId = thinkingOptionId,
) {
    override suspend fun createRequest(
        context: Context,
        requestBody: RequestBody,
        isStreaming: Boolean,
        requestId: String,
    ): Request {
        val inner = JSONObject(requestBodyToString(requestBody))
        val projectId = authManager.currentProjectId()
            ?: throw IllegalStateException("Antigravity project id is unavailable")
        val runtimeModel = AntigravityOAuthProtocol.runtimeModelId(modelName)
        val isImageModel = modelName.contains("image", ignoreCase = true)
        val requestType = if (isImageModel) "image_gen" else "agent"

        // Intent: spoof the request-ID format per CLIProxyAPI (image_gen for image gen, agent-<uuid> for chat).
        // If skipped: an operit- prefix is trivially fingerprinted as a third-party client in upstream logs.
        val reqId = if (isImageModel) {
            "image_gen/${System.currentTimeMillis()}/${UUID.randomUUID()}/12"
        } else {
            "agent-${UUID.randomUUID()}"
        }

        // Intent: strip safetySettings — the Antigravity gateway rejects unknown/unexpected safety fields.
        inner.remove("safetySettings")

        // Intent: derive a stable negative session id from the first user message to mimic native session tracking.
        if (!isImageModel) {
            val sessionId = deriveSessionId(inner)
            inner.put("sessionId", sessionId)
        }

        // Intent: set functionCallingConfig.mode=VALIDATED for Claude models, per CLIProxyAPI.
        if (runtimeModel.contains("claude", ignoreCase = true)) {
            val toolConfig = inner.optJSONObject("toolConfig") ?: JSONObject().also { inner.put("toolConfig", it) }
            val funcConfig = toolConfig.optJSONObject("functionCallingConfig") ?: JSONObject().also { toolConfig.put("functionCallingConfig", it) }
            funcConfig.put("mode", "VALIDATED")
        }

        val envelope = JSONObject()
            .put("project", projectId)
            .put("model", runtimeModel)
            .put("request", inner)
            .put("requestType", requestType)
            .put("userAgent", "antigravity")
            .put("requestId", reqId)

        val method = if (isStreaming) "streamGenerateContent?alt=sse" else "generateContent"
        val endpoint = apiEndpoint.ifBlank { AntigravityOAuthProtocol.DEFAULT_ENDPOINT }
        val url = endpoint.trimEnd('/') + "/v1internal:$method"
        val token = apiKeyProvider.getApiKey()

        // Intent: keep only required headers; drop X-Goog-Api-Client and Client-Metadata which expose plugin identity.
        val builder = Request.Builder()
            .url(url)
            .post(envelope.toString().toRequestBody(JSON))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Accept", if (isStreaming) "text/event-stream" else "application/json")
            .header("User-Agent", AntigravityOAuthProtocol.USER_AGENT)

        customHeaders.forEach { (key, value) -> builder.header(key, value) }
        return builder.build()
    }

    override suspend fun getModelsList(_context: Context): Result<List<ModelOption>> {
        return authManager.fetchModels().map { models ->
            models.ifEmpty {
                AntigravityOAuthProtocol.defaultModels.map { (id, name) ->
                    ModelOption(id = id, name = name)
                }
            }
        }.recover {
            AntigravityOAuthProtocol.defaultModels.map { (id, name) ->
                ModelOption(id = id, name = name)
            }
        }
    }

    override fun unwrapStreamingPayload(json: JSONObject): JSONObject {
        return json.optJSONObject("response") ?: json
    }

    // Intent: derive a stable session id from the first user message so one turn keeps a continuous session.
    private fun deriveSessionId(request: JSONObject): String {
        val contents = request.optJSONArray("contents") ?: return generateRandomSessionId()
        for (i in 0 until contents.length()) {
            val content = contents.optJSONObject(i) ?: continue
            if (content.optString("role") == "user") {
                val parts = content.optJSONArray("parts") ?: continue
                val firstPart = parts.optJSONObject(0) ?: continue
                val text = firstPart.optString("text")
                if (text.isNotBlank()) {
                    val hash = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(StandardCharsets.UTF_8))
                    var value = 0L
                    for (b in 0 until 8) {
                        value = (value shl 8) or (hash[b].toLong() and 0xFFL)
                    }
                    value = value and 0x7FFFFFFFFFFFFFFFL
                    return "-$value"
                }
            }
        }
        return generateRandomSessionId()
    }

    private fun generateRandomSessionId(): String {
        val random = SecureRandom()
        val n = (random.nextLong() and 0x7FFFFFFFFFFFFFFFL) % 9_000_000_000_000_000_000L
        return "-$n"
    }

    private fun requestBodyToString(requestBody: RequestBody): String {
        val buffer = okio.Buffer()
        requestBody.writeTo(buffer)
        return buffer.readUtf8()
    }
}
