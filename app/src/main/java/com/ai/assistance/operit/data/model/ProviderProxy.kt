package com.ai.assistance.operit.data.model

import com.ai.assistance.operit.util.AppLogger
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.SocketAddress
import javax.net.SocketFactory
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient

/**
 * Provider-level proxy support (SOCKS5 + HTTP CONNECT).
 *
 * Intent: route a single provider config's traffic through a proxy without
 * touching the shared base clients. If skipped: users behind a proxy-only
 * network cannot reach remote providers at all.
 *
 * Design notes:
 * - Settings live on [ModelConfigData] so every config carries its own proxy.
 * - HTTP proxies are native OkHttp (`Proxy.Type.HTTP` + optional 407 auth).
 * - SOCKS5 without auth is native OkHttp; WITH auth OkHttp has no support,
 *   so we tunnel via [Socks5TunnelSocketFactory] (RFC 1928 + RFC 1929).
 * - Proxied clients derive from the caller's base via `newBuilder()`, so pool,
 *   dispatcher, timeouts and listeners are shared, not duplicated.
 */
object ProviderProxyDefaults {
    const val MODE_DIRECT = "DIRECT"
    const val MODE_HTTP = "HTTP"
    const val MODE_SOCKS5 = "SOCKS5"

    // Lab proxies baked in while the fork is single-user (user-provided 14.10).
    // If skipped: the user retypes these per config; DIRECT stays the default
    // mode so nothing routes through them until explicitly enabled.
    const val HOST = "192.168.1.55"
    const val HTTP_PORT = 18080
    const val SOCKS_PORT = 10808
}

/** Proxy settings snapshot; mirrors the proxy fields of [ModelConfigData]. */
data class ProxySettings(
        val mode: String = ProviderProxyDefaults.MODE_DIRECT,
        val host: String = ProviderProxyDefaults.HOST,
        val port: Int = ProviderProxyDefaults.HTTP_PORT,
        val username: String = "",
        val password: String = ""
) {
    val isDirect: Boolean
        get() = mode != ProviderProxyDefaults.MODE_HTTP && mode != ProviderProxyDefaults.MODE_SOCKS5
}

fun ModelConfigData.proxySettings(): ProxySettings =
        ProxySettings(
                mode = proxyMode,
                host = proxyHost,
                port = proxyPort,
                username = proxyUsername,
                password = proxyPassword
        )

private const val TAG = "ProviderProxy"

/**
 * SocketFactory whose sockets CONNECT-tunnel through a SOCKS5 proxy
 * (RFC 1928 greeting/method + RFC 1929 username/password + CONNECT).
 *
 * Intent: OkHttp cannot do authenticated SOCKS5 natively. If skipped:
 * SOCKS5-with-auth configs silently fail at TCP level.
 *
 * The trick: [Socks5TunnelSocket.connect] dials the PROXY (not the target)
 * on the real socket, performs the handshake asking for CONNECT to the
 * target, and returns with the socket itself as the tunnel — all later I/O
 * (TLS, HTTP) flows through it transparently.
 */
internal class Socks5TunnelSocket(
        private val proxyHost: String,
        private val proxyPort: Int,
        private val username: String,
        private val password: String,
        private val handshakeTimeoutMillis: Int
) : Socket() {

    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        val target =
                endpoint as? InetSocketAddress
                        ?: throw IOException("SOCKS5 tunnel requires an InetSocketAddress target")
        try {
            super.connect(InetSocketAddress(proxyHost, proxyPort), timeout)
            soTimeout = handshakeTimeoutMillis.coerceAtLeast(1)
            performHandshake(target)
            soTimeout = 0
        } catch (e: IOException) {
            try {
                close()
            } catch (closeError: IOException) {
                AppLogger.w(TAG, "Error closing failed SOCKS5 tunnel to $proxyHost:$proxyPort", closeError)
            }
            throw e
        }
    }

    private fun performHandshake(target: InetSocketAddress) {
        val out = getOutputStream()
        val input = getInputStream()
        val useAuth = username.isNotEmpty()

        out.write(if (useAuth) byteArrayOf(0x05, 0x02, 0x00, 0x02) else byteArrayOf(0x05, 0x01, 0x00))
        out.flush()

        val methodReply = input.readExactly(2)
        if (methodReply[0] != 0x05.toByte()) {
            throw IOException("SOCKS5 proxy $proxyHost:$proxyPort returned bad version ${methodReply[0]}")
        }
        when (methodReply[1].toInt() and 0xFF) {
            0x00 -> Unit
            0x02 -> {
                if (!useAuth) {
                    throw IOException("SOCKS5 proxy $proxyHost:$proxyPort demands username/password authentication")
                }
                val userBytes = username.toByteArray(Charsets.UTF_8)
                val passBytes = password.toByteArray(Charsets.UTF_8)
                if (userBytes.size > 255 || passBytes.size > 255) {
                    throw IOException("SOCKS5 credentials exceed the 255-byte RFC 1929 limit")
                }
                out.write(
                        byteArrayOf(0x01, userBytes.size.toByte()) + userBytes +
                                byteArrayOf(passBytes.size.toByte()) + passBytes
                )
                out.flush()
                val authReply = input.readExactly(2)
                if (authReply[1] != 0x00.toByte()) {
                    throw IOException("SOCKS5 proxy $proxyHost:$proxyPort rejected the username/password")
                }
            }
            0xFF -> throw IOException("SOCKS5 proxy $proxyHost:$proxyPort offers no acceptable auth method")
            else -> throw IOException("SOCKS5 proxy selected unsupported method ${methodReply[1].toInt() and 0xFF}")
        }

        val address = target.address
        val addrPart =
                if (address != null) {
                    val raw = address.address
                    val atyp = if (raw.size == 4) 0x01 else 0x04
                    byteArrayOf(atyp.toByte()) + raw
                } else {
                    // Unresolved target: let the proxy resolve the domain (ATYP 0x03).
                    val nameBytes = target.hostString.toByteArray(Charsets.UTF_8)
                    if (nameBytes.isEmpty() || nameBytes.size > 255) {
                        throw IOException("SOCKS5 target hostname is not encodable: '${target.hostString}'")
                    }
                    byteArrayOf(0x03, nameBytes.size.toByte()) + nameBytes
                }
        val portBytes =
                byteArrayOf(((target.port ushr 8) and 0xFF).toByte(), (target.port and 0xFF).toByte())
        out.write(byteArrayOf(0x05, 0x01, 0x00) + addrPart + portBytes)
        out.flush()

        val header = input.readExactly(4)
        if (header[0] != 0x05.toByte()) {
            throw IOException("SOCKS5 proxy $proxyHost:$proxyPort returned bad CONNECT version ${header[0]}")
        }
        if (header[1] != 0x00.toByte()) {
            throw IOException(
                    "SOCKS5 CONNECT to ${target.hostString}:${target.port} rejected (code ${header[1].toInt() and 0xFF})"
            )
        }
        val boundSize =
                when (header[3].toInt() and 0xFF) {
                    0x01 -> 4 + 2
                    0x04 -> 16 + 2
                    0x03 -> (input.readExactly(1)[0].toInt() and 0xFF) + 2
                    else ->
                            throw IOException(
                                    "SOCKS5 proxy returned unknown address type ${header[3].toInt() and 0xFF}"
                            )
                }
        input.readExactly(boundSize)
        AppLogger.d(TAG, "SOCKS5 tunnel via $proxyHost:$proxyPort to ${target.hostString}:${target.port} established")
    }

    private fun InputStream.readExactly(n: Int): ByteArray {
        val buf = ByteArray(n)
        var off = 0
        while (off < n) {
            val read = read(buf, off, n - off)
            if (read == -1) {
                throw IOException("SOCKS5 proxy $proxyHost:$proxyPort closed the connection mid-handshake")
            }
            off += read
        }
        return buf
    }
}

internal class Socks5TunnelSocketFactory(
        private val proxyHost: String,
        private val proxyPort: Int,
        private val username: String,
        private val password: String,
        private val handshakeTimeoutMillis: Int
) : SocketFactory() {
    override fun createSocket(): Socket =
            Socks5TunnelSocket(proxyHost, proxyPort, username, password, handshakeTimeoutMillis)

    // Defensive: every socket from this factory must tunnel, never go direct.
    override fun createSocket(host: String?, port: Int): Socket =
            createSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: String?, port: Int, localHost: InetSocketAddress?, localPort: Int): Socket =
            createSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: java.net.InetAddress?, port: Int): Socket =
            createSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(
            address: java.net.InetAddress?,
            port: Int,
            localAddress: java.net.InetAddress?,
            localPort: Int
    ): Socket = createSocket().apply { connect(InetSocketAddress(address, port)) }
}

/** Builds (and caches) per-proxy OkHttpClient variants of any base client. */
object ProviderProxyClients {
    private val lock = Any()
    // Key includes the base client identity: different bases (chat pool vs
    // OAuth clients) must never share a variant.
    private val cache = mutableMapOf<Pair<OkHttpClient, ProxySettings>, OkHttpClient>()

    fun forBase(base: OkHttpClient, settings: ProxySettings): OkHttpClient {
        if (settings.isDirect) return base
        val host = settings.host.trim()
        val port = settings.port
        if (host.isEmpty() || port <= 0 || port > 65535) {
            // Intent: misconfigured proxy must degrade loudly to direct, not
            // hang on an undialable address. If skipped: a typo'd host blocks
            // the provider with an opaque connect timeout.
            AppLogger.w(TAG, "Proxy ${settings.mode} misconfigured (host='$host', port=$port) — using a direct connection")
            return base
        }
        synchronized(lock) {
            cache[base to settings]?.let { return it }
        }
        val built =
                when (settings.mode) {
                    ProviderProxyDefaults.MODE_HTTP -> {
                        val builder = base.newBuilder().proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
                        if (settings.username.isNotEmpty()) {
                            builder.proxyAuthenticator(proxyAuthenticator(settings.username, settings.password))
                        }
                        builder.build()
                    }
                    ProviderProxyDefaults.MODE_SOCKS5 -> {
                        val builder = base.newBuilder().proxy(Proxy.NO_PROXY)
                        if (settings.username.isNotEmpty()) {
                            builder.socketFactory(
                                    Socks5TunnelSocketFactory(
                                            proxyHost = host,
                                            proxyPort = port,
                                            username = settings.username,
                                            password = settings.password,
                                            handshakeTimeoutMillis = base.connectTimeoutMillis
                                    )
                            )
                        } else {
                            builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port)))
                        }
                        builder.build()
                    }
                    else -> base
                }
        if (built !== base) {
            AppLogger.d(TAG, "Built ${settings.mode} client via $host:$port")
        }
        synchronized(lock) {
            if (built !== base) {
                cache[base to settings] = built
            }
        }
        return built
    }

    fun forConfig(base: OkHttpClient, config: ModelConfigData): OkHttpClient =
            forBase(base, config.proxySettings())

    private fun proxyAuthenticator(username: String, password: String): Authenticator =
            Authenticator { _, response ->
                if (response.request.header("Proxy-Authorization") != null) {
                    // Already tried: do not loop on a 407.
                    null
                } else {
                    response.request
                            .newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(username, password))
                            .build()
                }
            }
}
