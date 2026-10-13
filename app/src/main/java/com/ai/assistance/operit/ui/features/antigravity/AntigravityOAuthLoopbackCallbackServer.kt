package com.ai.assistance.operit.ui.features.antigravity

import com.ai.assistance.operit.data.api.AntigravityOAuthProtocol
import com.ai.assistance.operit.util.AppLogger
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal class AntigravityOAuthLoopbackCallbackServer internal constructor(
    private val serverSocket: ServerSocket,
    private val requestReadTimeoutMillis: Int = 30_000,
    private val log: (String) -> Unit = { AppLogger.d(TAG, it) },
) {
    val redirectUri: String = "http://localhost:${serverSocket.localPort}${AntigravityOAuthProtocol.CALLBACK_PATH}"
    private val socketLock = Any()
    private var activeSocket: Socket? = null

    suspend fun awaitCallback(expectedState: String): String = withContext(Dispatchers.IO) {
        try {
            suspendCancellableCoroutine { continuation ->
                // accept/read do not exit on coroutine cancellation — the listener and active connection must be closed explicitly.
                continuation.invokeOnCancellation { close() }
                try {
                    continuation.resume(awaitCallbackBlocking(expectedState))
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            }
        } finally {
            close()
        }
    }

    private fun awaitCallbackBlocking(expectedState: String): String {
        require(expectedState.isNotBlank())
        while (!serverSocket.isClosed) {
            val socket = serverSocket.accept()
            synchronized(socketLock) {
                if (serverSocket.isClosed) {
                    closeSocket(socket)
                    throw IOException("OAuth callback listener closed")
                }
                activeSocket = socket
            }
            try {
                val callback = handleRequest(socket, expectedState)
                if (callback != null) return callback
            } catch (_: SocketTimeoutException) {
                log("Callback request read timed out; still waiting for authorization")
            } catch (_: IOException) {
                // Browser pre-connects, refreshes or disconnects affect only this connection, never the whole login session.
                log("Callback connection closed before a complete request; still waiting")
            } finally {
                synchronized(socketLock) { activeSocket = null }
                closeSocket(socket)
            }
        }
        throw IOException("OAuth callback listener closed")
    }

    fun close() {
        synchronized(socketLock) {
            try {
                serverSocket.close()
            } catch (_: IOException) {
                // Double-close is harmless to the cancellation flow.
            }
            activeSocket?.let(::closeSocket)
        }
    }

    internal fun handleRequest(socket: Socket, expectedState: String): String? {
        val requestLine = readRequestLineAndHeaders(socket) ?: return null
        val parts = requestLine.split(' ', limit = 3)
        if (parts.size != 3 || parts[0] != "GET" || parts[2] !in listOf("HTTP/1.0", "HTTP/1.1")) {
            writeResponseSafely(socket, "400 Bad Request", "Invalid callback request.")
            return null
        }
        val requestUri = try {
            URI(parts[1])
        } catch (_: java.net.URISyntaxException) {
            writeResponseSafely(socket, "400 Bad Request", "Invalid callback request.")
            return null
        }
        if (requestUri.isAbsolute || requestUri.rawAuthority != null || requestUri.rawFragment != null) {
            writeResponseSafely(socket, "400 Bad Request", "Invalid callback request.")
            return null
        }
        if (requestUri.rawPath != AntigravityOAuthProtocol.CALLBACK_PATH) {
            log("Ignoring request to a non-callback path")
            writeResponseSafely(socket, "404 Not Found", "Not found.")
            return null
        }
        val fields = try {
            requestUri.rawQuery.orEmpty().split('&').map { field ->
                val pair = field.split('=', limit = 2)
                URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
            }.groupBy({ it.first }, { it.second })
        } catch (_: IllegalArgumentException) {
            writeResponseSafely(socket, "400 Bad Request", "Invalid callback parameters.")
            return null
        }
        val state = fields["state"]?.singleOrNull()
        val hasCode = !fields["code"]?.singleOrNull().isNullOrBlank()
        val hasError = !fields["error"]?.singleOrNull().isNullOrBlank()
        if (state != expectedState || !(hasCode xor hasError) ||
            (hasCode && fields.containsKey("error")) || (hasError && fields.containsKey("code"))) {
            log("Ignoring callback with invalid state or authorization parameters")
            writeResponseSafely(socket, "400 Bad Request", "Invalid or expired callback. Return to Operit.")
            return null
        }
        // Once the callback validates, a failed prompt page must not discard the received auth code.
        log("Validated OAuth callback received")
        writeResponseSafely(socket, "200 OK", "Authorization response received. Return to Operit to finish signing in.")
        return "$redirectUri?${requestUri.rawQuery}"
    }

    private fun readRequestLineAndHeaders(socket: Socket): String? {
        val input = socket.getInputStream().buffered()
        val header = StringBuilder()
        val deadline = System.nanoTime() + requestReadTimeoutMillis * 1_000_000L
        // Drain request headers fully before replying to avoid RST on unread data; cap size and total read time.
        while (header.length < MAX_HEADER_BYTES) {
            val remainingMillis = (deadline - System.nanoTime()) / 1_000_000L
            if (remainingMillis <= 0) throw SocketTimeoutException("Callback request read timed out")
            socket.soTimeout = remainingMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
            val value = input.read()
            if (value < 0) return null
            header.append(value.toChar())
            if (header.endsWith("\r\n\r\n")) {
                return header.toString().substringBefore("\r\n")
            }
        }
        writeResponseSafely(socket, "431 Request Header Fields Too Large", "Callback request headers too large.")
        return null
    }

    private fun writeResponseSafely(socket: Socket, status: String, message: String) {
        val body = "<!doctype html><html><body>$message</body></html>".toByteArray(StandardCharsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ").append(status).append("\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("Cache-Control: no-store\r\n")
            append("Content-Length: ").append(body.size).append("\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.US_ASCII)
        try {
            socket.getOutputStream().apply {
                write(header)
                write(body)
                flush()
            }
        } catch (_: IOException) {
            log("Browser disconnected while receiving callback response")
        }
    }

    private fun closeSocket(socket: Socket) {
        try {
            socket.close()
        } catch (_: IOException) {
            // A failed connection cleanup must not overwrite the already-received callback result.
        }
    }

    companion object {
        private const val TAG = "AntigravityOAuthCallback"
        private const val MAX_HEADER_BYTES = 16 * 1024

        fun open(): AntigravityOAuthLoopbackCallbackServer {
            return AntigravityOAuthLoopbackCallbackServer(
                ServerSocket(AntigravityOAuthProtocol.CALLBACK_PORT, 8, InetAddress.getByName("127.0.0.1")),
            )
        }
    }
}
