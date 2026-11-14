package com.student.amizoneplus.data

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One cookie store for everything: OkHttp reads and writes Android's WebView
 * CookieManager, so the app's own connection and its in-app web pages share a session.
 */
class WebViewCookieJar : CookieJar {
    private val cm: CookieManager = CookieManager.getInstance().apply { setAcceptCookie(true) }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (c in cookies) cm.setCookie(url.toString(), Http.keep(c).toString())
        cm.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val header = cm.getCookie(url.toString()) ?: return emptyList()
        return header.split(';').mapNotNull { Cookie.parse(url, it.trim()) }
    }
}

object Http {
    /**
     * The exact User-Agent of this phone's WebView. The app's connection and the web
     * view MUST present the same identity: Amizone sits behind Cloudflare, which ties
     * its clearance (and so the session) to the browser's User-Agent.
     */
    var UA: String = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
        private set

    fun init(context: Context) {
        runCatching { UA = WebSettings.getDefaultUserAgent(context.applicationContext) }
    }

    private const val KEEP_MS = 14L * 86_400_000

    /**
     * Amizone's sign-in cookies are "session" cookies, which the phone may forget when
     * the app is closed — then every launch needs a fresh sign-in. Keep them for 14 days.
     */
    fun keep(c: Cookie): Cookie {
        if (c.persistent || !c.domain.endsWith("amizone.net")) return c
        return Cookie.Builder().name(c.name).value(c.value).path(c.path)
            .apply { if (c.hostOnly) hostOnlyDomain(c.domain) else domain(c.domain) }
            .apply { if (c.secure) secure(); if (c.httpOnly) httpOnly() }
            .expiresAt(System.currentTimeMillis() + KEEP_MS)
            .build()
    }

    /** Same, for cookies the web view set itself (after a website sign-in). */
    fun persistSession() {
        val cm = CookieManager.getInstance()
        val base = AmizoneClient.BASE
        val header = cm.getCookie(base) ?: return
        for (part in header.split(';')) {
            val kv = part.trim()
            if (!kv.contains('=')) continue
            cm.setCookie(base, "$kv; Path=/; Max-Age=${KEEP_MS / 1000}; Secure; HttpOnly")
        }
        cm.flush()
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(WebViewCookieJar())
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", UA).build()) }
            .build()
    }
}
