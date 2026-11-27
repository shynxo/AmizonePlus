package com.student.amizoneplus.data

import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Build
import android.webkit.SslErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.security.MessageDigest

/**
 * Lets the in-app web pages open Amizone on older phones.
 *
 * Amizone's certificate chains up to "GlobalSign Root R46", which Android 8 / 9 don't
 * trust. The app's own connection (OkHttp) trusts that root for amizone.net through
 * res/xml/network_security_config.xml, but WebView ignores that file and reports
 * SSL_UNTRUSTED. In that one case we ask OkHttp to connect to the same host. It does a
 * full check (chain to the bundled root, dates, host name). If the certificate WebView was
 * shown is byte-for-byte the one OkHttp just verified, the page may proceed. Anything else
 * (another host, another kind of error, a different certificate) is refused.
 */
object AmizoneSsl {
    private fun isAmizone(host: String) = host == "amizone.net" || host.endsWith(".amizone.net")

    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun leafBytes(c: SslCertificate?): ByteArray? {
        c ?: return null
        if (Build.VERSION.SDK_INT >= 29) return runCatching { c.x509Certificate?.encoded }.getOrNull()
        return runCatching { SslCertificate.saveState(c).getByteArray("x509-certificate") }.getOrNull()
    }

    /** SHA-256 of the certificate [host] presents, if it passes OkHttp's full verification; else null. */
    private suspend fun verifiedLeaf(host: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            Http.client.newCall(Request.Builder().url("https://$host/").head().build()).execute().use { r ->
                r.handshake?.peerCertificates?.firstOrNull()?.encoded?.let(::sha256)
            }
        }.getOrNull()
    }

    /** Call from WebViewClient.onReceivedSslError. */
    fun handle(handler: SslErrorHandler, error: SslError) {
        val host = runCatching { android.net.Uri.parse(error.url).host.orEmpty() }.getOrDefault("")
        val leaf = leafBytes(error.certificate)
        if (error.primaryError != SslError.SSL_UNTRUSTED || !isAmizone(host) || leaf == null) {
            handler.cancel(); return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val ok = verifiedLeaf(host) == sha256(leaf)
            SessionLog.add("WEBVIEW untrusted cert for $host: ${if (ok) "verified with bundled root, proceeding" else "NOT verified, blocked"}")
            if (ok) handler.proceed() else handler.cancel()
        }
    }
}
