package com.ai.assistance.operit.ui.features.antigravity

import android.content.Context
import android.net.Uri
import com.ai.assistance.operit.data.api.AntigravityAuthManager
import com.ai.assistance.operit.data.api.AntigravityOAuthClient
import com.ai.assistance.operit.data.api.AntigravityOAuthProtocol
import com.ai.assistance.operit.data.api.AntigravityPkceCodes
import com.ai.assistance.operit.data.model.ProviderProxyClients
import com.ai.assistance.operit.data.model.ProxySettings
import com.ai.assistance.operit.data.preferences.AntigravityAuthState
import com.ai.assistance.operit.util.AppLogger
import java.io.IOException

internal data class AntigravityOAuthLoginSession(
    internal val callbackServer: AntigravityOAuthLoopbackCallbackServer,
    internal val pkce: AntigravityPkceCodes,
    internal val state: String,
    val authorizationUrl: String,
    val expiresAt: Long,
) {
    val redirectUri: String
        get() = callbackServer.redirectUri
}

internal class AntigravityOAuthCoordinator(context: Context) {
    private val authManager = AntigravityAuthManager.getInstance(context)

    // Intent: the loopback callback server below stays direct (phone-local
    // listener, not provider traffic). Everything addressed TO the provider —
    // token exchange, email, project discovery, onboarding — goes through
    // the caller's config proxy.
    private val baseHttpClient = okhttp3.OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .connectionPool(okhttp3.ConnectionPool(2, 30, java.util.concurrent.TimeUnit.SECONDS))
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private fun oauthClient(proxy: ProxySettings?): AntigravityOAuthClient =
        AntigravityOAuthClient(
            client = if (proxy == null) baseHttpClient else ProviderProxyClients.forBase(baseHttpClient, proxy),
        )

    suspend fun startLogin(): AntigravityOAuthLoginSession {
        val callbackServer = AntigravityOAuthLoopbackCallbackServer.open()
        try {
            val pkce = AntigravityOAuthProtocol.generatePkce()
            val state = AntigravityOAuthProtocol.generateState()
            val expiresAt = System.currentTimeMillis() + AntigravityOAuthProtocol.OAUTH_TIMEOUT_MILLIS
            return AntigravityOAuthLoginSession(
                callbackServer = callbackServer,
                pkce = pkce,
                state = state,
                authorizationUrl = AntigravityOAuthProtocol.buildAuthorizationUrl(
                    redirectUri = callbackServer.redirectUri,
                    pkce = pkce,
                    state = state,
                ),
                expiresAt = expiresAt,
            )
        } catch (error: Exception) {
            callbackServer.close()
            throw error
        }
    }

    suspend fun completeLogin(
        session: AntigravityOAuthLoginSession,
        callbackUri: Uri,
        proxy: ProxySettings?,
    ): AntigravityAuthState {
        val callbackState = callbackUri.getQueryParameter("state")
        if (callbackState != session.state) {
            throw IOException("Antigravity OAuth state does not match")
        }
        val error = callbackUri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            val description = callbackUri.getQueryParameter("error_description")
            throw IOException(description ?: error)
        }
        val code = callbackUri.getQueryParameter("code")
            ?: throw IOException("Antigravity OAuth callback has no authorization code")
        AppLogger.d(TAG, "OAuth callback received; exchanging authorization code")
        val tokens = oauthClient(proxy).exchangeAuthorizationCode(
            code = code,
            redirectUri = session.redirectUri,
            verifier = session.pkce.verifier,
        )
        AppLogger.d(TAG, "OAuth token exchange succeeded; loading account metadata")
        return authManager.saveLoginTokens(tokens, proxy)
    }

    companion object {
        private const val TAG = "AntigravityOAuthCoordinator"
    }
}
