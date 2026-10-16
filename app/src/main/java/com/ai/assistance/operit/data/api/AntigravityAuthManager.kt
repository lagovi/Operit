package com.ai.assistance.operit.data.api

import android.content.Context
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.model.ProviderProxyClients
import com.ai.assistance.operit.data.model.ProxySettings
import com.ai.assistance.operit.data.preferences.AntigravityAuthPreferences
import com.ai.assistance.operit.data.preferences.AntigravityAuthState
import com.ai.assistance.operit.data.preferences.AntigravityQuotaPreferences
import com.ai.assistance.operit.data.preferences.AntigravityStoredQuotaSnapshot
import com.ai.assistance.operit.util.AppLogger
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AntigravityAuthManager private constructor(context: Context) {
    private val preferences = AntigravityAuthPreferences.getInstance(context)

    // Intent: force HTTP/1.1 with max 2 connections per host (30s idle timeout).
    // If skipped: native Antigravity avoids HTTP/2 without proxy/special directives, and long-idle connections trip the GFE 240s reset.
    // NOTE: every account-level flow (token exchange/refresh, email, project
    // discovery, onboarding) honors the caller's config proxy — when a proxy
    // is configured, ALL provider traffic goes through it, no direct bits.
    private val baseHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectionPool(ConnectionPool(2, 30, TimeUnit.SECONDS))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun oauthClient(proxy: ProxySettings?) = AntigravityOAuthClient(client = clientFor(proxy))

    private fun clientFor(proxy: ProxySettings?): OkHttpClient =
        if (proxy == null) baseHttpClient else ProviderProxyClients.forBase(baseHttpClient, proxy)

    private fun modelClient(proxy: ProxySettings?) = AntigravityModelClient(client = clientFor(proxy))

    private fun quotaClient(proxy: ProxySettings?) = AntigravityQuotaClient(client = clientFor(proxy))
    private val quotaPreferences = AntigravityQuotaPreferences.getInstance(context)
    private val refreshMutex = Mutex()

    val authState: StateFlow<AntigravityAuthState?> = preferences.authState
    val quotaSnapshotFlow: Flow<AntigravityStoredQuotaSnapshot?> = quotaPreferences.snapshotFlow

    suspend fun saveLoginTokens(tokens: AntigravityOAuthTokenResponse, proxy: ProxySettings? = null): AntigravityAuthState {
        val accessToken = tokens.accessToken
            ?: throw IOException("Antigravity OAuth response has no access token")
        val refreshToken = tokens.refreshToken
            ?: throw IOException("Antigravity OAuth response has no refresh token")
        val expiresIn = tokens.expiresInSeconds
            ?: throw IOException("Antigravity OAuth response has no expiration")
        AppLogger.d(TAG, "Fetching account email after token exchange")
        val email = fetchEmail(accessToken, proxy)
            ?: throw IOException("Antigravity account email is unavailable")
        AppLogger.d(TAG, "Discovering project after account email lookup")

        // Intent: resolve the real GCP project id first; for a fresh unactivated account run the onboardUser activation flow.
        // If skipped: no fabricated local-UUID fallback is allowed — generate requests without a real project id are rejected upstream.
        val projectId = discoverProjectId(accessToken, proxy)
        val state = AntigravityAuthState(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L -
                AntigravityOAuthProtocol.EXPIRY_SKEW_MILLIS,
            projectId = projectId,
            email = email,
        )
        currentCoroutineContext().ensureActive()
        preferences.save(state)
        AppLogger.d(TAG, "Antigravity login credentials saved for project: $projectId")
        return state
    }

    suspend fun getValidAccessToken(proxy: ProxySettings? = null): String {
        val current = preferences.currentState()
            ?: throw IOException("Antigravity is not logged in")
        if (current.expiresAtMillis - System.currentTimeMillis() > REFRESH_WINDOW_MILLIS) {
            return current.accessToken
        }
        return refreshMutex.withLock {
            val latest = preferences.currentState()
                ?: throw IOException("Antigravity is not logged in")
            if (latest.expiresAtMillis - System.currentTimeMillis() > REFRESH_WINDOW_MILLIS) {
                latest.accessToken
            } else {
                refreshAccessToken(latest, proxy).accessToken
            }
        }
    }

    fun currentProjectId(): String? = preferences.currentState()?.projectId

    suspend fun refreshAccessToken(current: AntigravityAuthState, proxy: ProxySettings? = null): AntigravityAuthState {
        val response = oauthClient(proxy).refreshAccessToken(current.refreshToken)
        val accessToken = response.accessToken
            ?: throw IOException("Antigravity refresh response has no access token")
        val expiresIn = response.expiresInSeconds
            ?: throw IOException("Antigravity refresh response has no expiration")
        val projectId = current.projectId.ifBlank {
            discoverProjectId(accessToken, proxy)
        }
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = response.refreshToken ?: current.refreshToken,
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L -
                AntigravityOAuthProtocol.EXPIRY_SKEW_MILLIS,
            projectId = projectId,
        )
        preferences.save(updated)
        return updated
    }

    suspend fun logout() {
        preferences.clear()
    }

    suspend fun fetchQuota(proxy: ProxySettings? = null): Result<AntigravityQuotaSnapshot> {
        return try {
            val accessToken = getValidAccessToken(proxy)
            val projectId = currentProjectId()
                ?: throw IOException("Antigravity project id is unavailable")
            val result = quotaClient(proxy).fetch(accessToken = accessToken, projectId = projectId)
            if (result.isSuccess) {
                quotaPreferences.save(projectId, result.getOrThrow())
            }
            result
        } catch (error: Exception) {
            AppLogger.e(TAG, "Failed to prepare Antigravity quota request", error)
            Result.failure(error)
        }
    }

    suspend fun fetchModels(proxy: ProxySettings? = null): Result<List<ModelOption>> {
        return try {
            val accessToken = getValidAccessToken(proxy)
            val projectId = currentProjectId()
                ?: throw IOException("Antigravity project id is unavailable")
            modelClient(proxy).fetch(accessToken = accessToken, projectId = projectId)
        } catch (error: Exception) {
            AppLogger.e(TAG, "Failed to prepare Antigravity model request", error)
            Result.failure(error)
        }
    }

    private suspend fun fetchEmail(accessToken: String, proxy: ProxySettings?): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(AntigravityOAuthProtocol.USERINFO_URL)
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", AntigravityOAuthProtocol.USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        clientFor(proxy).newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string().orEmpty()
            JSONObject(body).optString("email").takeIf { it.isNotBlank() }
        }
    }

    // Intent: extract the project id from loadCodeAssist/onboardUser payloads; accept both string and object shapes.
    // If skipped: the response may carry cloudaicompanionProject as { id: "..." } — string-only parsing loses the project id.
    private fun extractCloudaicompanionProject(data: JSONObject): String? {
        for (key in listOf("cloudaicompanionProject", "projectId", "project")) {
            val opt = data.opt(key) ?: continue
            when (opt) {
                is String -> {
                    val trimmed = opt.trim()
                    if (trimmed.isNotBlank()) return trimmed
                }
                is JSONObject -> {
                    val id = opt.optString("id").trim().ifBlank { opt.optString("projectId").trim() }
                    if (id.isNotBlank()) return id
                }
            }
        }
        return null
    }

    private fun defaultTierId(loadResp: JSONObject): String {
        val allowedTiers = loadResp.optJSONArray("allowedTiers")
        if (allowedTiers != null) {
            for (i in 0 until allowedTiers.length()) {
                val tier = allowedTiers.optJSONObject(i) ?: continue
                if (tier.optBoolean("isDefault", false)) {
                    val id = tier.optString("id").trim()
                    if (id.isNotBlank()) return id
                }
            }
        }
        val currentTier = loadResp.optJSONObject("currentTier")
        val currentId = currentTier?.optString("id")?.trim().orEmpty()
        if (currentId.isNotBlank()) return currentId
        return "free-tier"
    }

    // Intent: for accounts with no GCP project yet, poll onboardUser to finish Google-side init and get the real project id.
    // If skipped: first-time Google accounts have no project in loadCodeAssist, so inference is impossible without activation.
    private suspend fun onboardUser(accessToken: String, tierId: String, proxy: ProxySettings?): String = withContext(Dispatchers.IO) {
        AppLogger.i(TAG, "Antigravity: onboarding user with tier: $tierId")
        val requestBody = JSONObject()
            .put("tier_id", tierId)
            .put(
                "metadata",
                JSONObject()
                    .put("ide_type", "ANTIGRAVITY")
                    .put("ide_version", AntigravityOAuthProtocol.HUB_VERSION)
                    .put("ide_name", "antigravity"),
            )
        val endpoint = "${AntigravityOAuthProtocol.DEFAULT_ENDPOINT.trimEnd('/')}/v1internal:onboardUser"

        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            currentCoroutineContext().ensureActive()
            AppLogger.d(TAG, "OnboardUser polling attempt $attempt/$maxAttempts")
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody.toString().toRequestBody(JSON_MEDIA))
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "*/*")
                .header("Content-Type", "application/json")
                .header("User-Agent", AntigravityOAuthProtocol.ONBOARD_USER_AGENT)
                .header("X-Goog-Api-Client", AntigravityOAuthProtocol.ONBOARD_GOOG_API_CLIENT)
                .build()

            val bodyText = clientFor(proxy).newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Antigravity onboardUser failed with HTTP ${response.code}: $body")
                }
                body
            }

            val json = JSONObject(bodyText)
            if (json.optBoolean("done", false)) {
                val respObj = json.optJSONObject("response")
                val project = respObj?.let(::extractCloudaicompanionProject)
                if (!project.isNullOrBlank()) {
                    AppLogger.i(TAG, "Successfully onboarded user and fetched project_id: $project")
                    return@withContext project
                }
                throw IOException("Antigravity onboardUser response missing project_id")
            }

            if (attempt < maxAttempts) {
                delay(2000L)
            }
        }
        throw IOException("Antigravity onboardUser did not complete after $maxAttempts attempts")
    }

    private suspend fun discoverProjectId(accessToken: String, proxy: ProxySettings?): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put(
            "metadata",
            JSONObject().put("ideType", "ANTIGRAVITY"),
        )
        val url = "${AntigravityOAuthProtocol.PROD_ENDPOINT.trimEnd('/')}/v1internal:loadCodeAssist"
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "*/*")
            .header("Content-Type", "application/json")
            .header("User-Agent", AntigravityOAuthProtocol.USER_AGENT)
            .build()

        val responseBody = clientFor(proxy).newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Antigravity loadCodeAssist failed with HTTP ${response.code}: $text")
            }
            text
        }

        val json = JSONObject(responseBody)
        val existingProject = extractCloudaicompanionProject(json)
        if (!existingProject.isNullOrBlank()) {
            return@withContext existingProject
        }

        // When loadCodeAssist returns no project id, onboard the user against the current available tier
        val tierId = defaultTierId(json)
        onboardUser(accessToken, tierId, proxy)
    }

    companion object {
        private const val TAG = "AntigravityAuthManager"
        private const val REFRESH_WINDOW_MILLIS = 60 * 1000L
        private val JSON_MEDIA = "application/json".toMediaType()

        @Volatile
        private var instance: AntigravityAuthManager? = null

        fun getInstance(context: Context): AntigravityAuthManager {
            return instance ?: synchronized(this) {
                instance ?: AntigravityAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
