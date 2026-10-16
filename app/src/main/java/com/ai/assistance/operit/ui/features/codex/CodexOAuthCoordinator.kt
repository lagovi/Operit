package com.ai.assistance.operit.ui.features.codex

import android.content.Context
import android.net.Uri
import com.ai.assistance.operit.data.api.CodexAuthManager
import com.ai.assistance.operit.data.api.CodexOAuthClient
import com.ai.assistance.operit.data.api.CodexOAuthProtocol
import com.ai.assistance.operit.data.api.CodexPkceCodes
import com.ai.assistance.operit.data.model.ProviderProxyClients
import com.ai.assistance.operit.data.model.ProxySettings
import com.ai.assistance.operit.data.preferences.CodexAuthState
import java.io.IOException

internal data class CodexOAuthLoginSession(
    internal val callbackServer: CodexOAuthLoopbackCallbackServer,
    internal val pkce: CodexPkceCodes,
    internal val state: String,
    val authorizationUrl: String,
    val expiresAt: Long,
) {
    val redirectUri: String
        get() = callbackServer.redirectUri
}

internal class CodexOAuthCoordinator(context: Context) {
    private val authManager = CodexAuthManager.getInstance(context)

    // Intent: the loopback callback server below stays direct (phone-local
    // listener, not provider traffic). The token exchange goes through the
    // caller's config proxy.
    private val baseHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private fun oauthClient(proxy: ProxySettings?): CodexOAuthClient =
        CodexOAuthClient(
            client = if (proxy == null) baseHttpClient else ProviderProxyClients.forBase(baseHttpClient, proxy),
        )

    suspend fun startLogin(): CodexOAuthLoginSession {
        val callbackServer = CodexOAuthLoopbackCallbackServer.open()
        val pkce = CodexOAuthProtocol.generatePkce()
        val state = CodexOAuthProtocol.generateState()
        val expiresAt = System.currentTimeMillis() + CodexOAuthProtocol.OAUTH_TIMEOUT_MILLIS
        return CodexOAuthLoginSession(
            callbackServer = callbackServer,
            pkce = pkce,
            state = state,
            authorizationUrl = CodexOAuthProtocol.buildAuthorizationUrl(
                redirectUri = callbackServer.redirectUri,
                pkce = pkce,
                state = state,
            ),
            expiresAt = expiresAt,
        )
    }

    suspend fun completeLogin(
        session: CodexOAuthLoginSession,
        callbackUri: Uri,
        proxy: ProxySettings?,
    ): CodexAuthState {
        val callbackState = callbackUri.getQueryParameter("state")
        if (callbackState != session.state) {
            throw IOException("Codex OAuth state does not match")
        }

        val error = callbackUri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            val description = callbackUri.getQueryParameter("error_description")
            throw IOException(description ?: error)
        }

        val code = callbackUri.getQueryParameter("code")
            ?: throw IOException("Codex OAuth callback has no authorization code")
        val tokens = oauthClient(proxy).exchangeAuthorizationCode(
            code = code,
            redirectUri = session.redirectUri,
            verifier = session.pkce.verifier,
        )
        return authManager.saveLoginTokens(tokens)
    }

    suspend fun logout(proxy: ProxySettings? = null) {
        authManager.logout(proxy)
    }

    fun cancel(session: CodexOAuthLoginSession) {
        session.callbackServer.close()
    }
}
