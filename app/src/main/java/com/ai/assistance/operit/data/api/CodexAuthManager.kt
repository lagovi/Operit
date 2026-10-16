package com.ai.assistance.operit.data.api

import android.content.Context
import com.ai.assistance.operit.data.preferences.CodexAuthPreferences
import com.ai.assistance.operit.data.preferences.CodexAuthState
import com.ai.assistance.operit.data.preferences.CodexStoredUsageSnapshot
import com.ai.assistance.operit.data.preferences.CodexUsagePreferences
import com.ai.assistance.operit.data.model.ProviderProxyClients
import com.ai.assistance.operit.data.model.ProxySettings
import com.ai.assistance.operit.util.AppLogger
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

class CodexAuthManager private constructor(context: Context) {
    private val preferences = CodexAuthPreferences.getInstance(context)
    // NOTE: every account-level flow (token exchange/refresh, revocation)
    // honors the caller's config proxy — when a proxy is configured, ALL
    // provider traffic goes through it, no direct bits.
    private val baseHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun oauthClient(proxy: ProxySettings?) = CodexOAuthClient(client = clientFor(proxy))

    private fun clientFor(proxy: ProxySettings?): OkHttpClient =
        if (proxy == null) baseHttpClient else ProviderProxyClients.forBase(baseHttpClient, proxy)

    private fun usageClient(proxy: ProxySettings?) = CodexUsageClient(client = clientFor(proxy))
    private val usagePreferences = CodexUsagePreferences.getInstance(context)
    private val refreshMutex = Mutex()

    val authState: StateFlow<CodexAuthState?> = preferences.authState
    val usageSnapshotFlow: Flow<CodexStoredUsageSnapshot?> = usagePreferences.snapshotFlow

    suspend fun saveLoginTokens(tokens: CodexOAuthTokenResponse): CodexAuthState {
        val completeTokens = tokens.requireComplete()
        val accessToken = requireNotNull(completeTokens.accessToken)
        val idToken = requireNotNull(completeTokens.idToken)
        val refreshToken = requireNotNull(completeTokens.refreshToken)
        val accessClaims = CodexOAuthProtocol.parseJwtClaims(accessToken)
            ?: throw IOException("Codex access token is not a valid JWT")
        val idClaims = CodexOAuthProtocol.parseJwtClaims(idToken)
        val accountId = idClaims?.accountId ?: accessClaims.accountId
            ?: throw IOException("Codex token has no ChatGPT account ID")
        val expiresAt = accessClaims.expiresAtMillis
            ?: completeTokens.expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
            ?: throw IOException("Codex access token has no expiration")
        val state = CodexAuthState(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = expiresAt,
            accountId = accountId,
            residency = accessClaims.residency ?: idClaims?.residency,
            email = idClaims?.email ?: accessClaims.email,
        )
        preferences.save(state)
        return state
    }

    suspend fun getValidAccessToken(proxy: ProxySettings? = null): String {
        val current = preferences.currentState()
            ?: throw IOException("Codex is not logged in")
        if (current.expiresAtMillis - System.currentTimeMillis() > REFRESH_WINDOW_MILLIS) {
            return current.accessToken
        }

        return refreshMutex.withLock {
            val latest = preferences.currentState()
                ?: throw IOException("Codex is not logged in")
            if (latest.expiresAtMillis - System.currentTimeMillis() > REFRESH_WINDOW_MILLIS) {
                latest.accessToken
            } else {
                refreshAccessToken(latest, proxy).accessToken
            }
        }
    }

    suspend fun refreshAccessToken(current: CodexAuthState, proxy: ProxySettings? = null): CodexAuthState {
        val response = oauthClient(proxy).refreshAccessToken(current.refreshToken)
        val accessToken = response.accessToken
            ?: throw IOException("Codex refresh response has no access token")
        val accessClaims = CodexOAuthProtocol.parseJwtClaims(accessToken)
            ?: throw IOException("Codex refreshed access token is not a valid JWT")
        val idClaims = response.idToken?.let(CodexOAuthProtocol::parseJwtClaims)
        val accountId = idClaims?.accountId ?: accessClaims.accountId ?: current.accountId
        val expiresAt = accessClaims.expiresAtMillis
            ?: response.expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
            ?: throw IOException("Codex refreshed access token has no expiration")
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = response.refreshToken ?: current.refreshToken,
            expiresAtMillis = expiresAt,
            accountId = accountId,
            residency = accessClaims.residency ?: idClaims?.residency ?: current.residency,
            email = idClaims?.email ?: accessClaims.email ?: current.email,
        )
        preferences.save(updated)
        return updated
    }

    suspend fun logout(proxy: ProxySettings? = null) {
        val current = preferences.currentState()
        try {
            if (current != null) {
                oauthClient(proxy).revokeToken(current.refreshToken, "refresh_token")
            }
        } catch (error: Exception) {
            AppLogger.e(TAG, "Failed to revoke Codex OAuth token", error)
        } finally {
            preferences.clear()
        }
    }

    fun currentAccountId(): String? = preferences.currentState()?.accountId

    fun currentResidency(): String? = preferences.currentState()?.residency

    suspend fun fetchUsage(proxy: ProxySettings? = null): Result<CodexUsageSnapshot> {
        return try {
            val accessToken = getValidAccessToken(proxy)
            val accountId = currentAccountId()
                ?: throw IOException("Codex account ID is unavailable")
            val result = usageClient(proxy).fetch(
                accessToken = accessToken,
                accountId = accountId,
                residency = currentResidency(),
            )
            if (result.isSuccess) {
                usagePreferences.save(accountId, result.getOrThrow())
            }
            result
        } catch (error: Exception) {
            AppLogger.e(TAG, "Failed to prepare Codex usage request", error)
            Result.failure(error)
        }
    }

    companion object {
        private const val TAG = "CodexAuthManager"
        private const val REFRESH_WINDOW_MILLIS = 5 * 60 * 1000L

        @Volatile
        private var instance: CodexAuthManager? = null

        fun getInstance(context: Context): CodexAuthManager {
            return instance ?: synchronized(this) {
                instance ?: CodexAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
