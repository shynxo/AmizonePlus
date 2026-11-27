package com.student.amizoneplus.data

import android.content.Context
import android.webkit.CookieManager
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A small journal of what happens to the Amizone session (pings, cookies Amizone sets,
 * when it expires), to find out why it lapses. It records cookie NAMES and a 6-character
 * fingerprint of each value, never the values themselves. Lives in the app's external
 * files folder (readable over USB debugging), capped at ~200 KB.
 */
object SessionLog {
    private var file: File? = null
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    fun init(context: Context) {
        file = context.getExternalFilesDir(null)?.let { File(it, "session-log.txt") }
    }

    @Synchronized
    fun add(line: String) {
        val f = file ?: return
        runCatching {
            if (f.length() > 200_000) f.writeText(f.readLines().takeLast(800).joinToString("\n", postfix = "\n"))
            f.appendText("${fmt.format(Date())} $line\n")
        }
    }

    fun fp(v: String): String =
        MessageDigest.getInstance("SHA-256").digest(v.toByteArray()).take(3).joinToString("") { "%02x".format(it) }

    /** e.g. ".ASPXAUTH=1a2b3c ASP.NET_SessionId=9f8e7d …", with ×2 when a name appears twice. */
    fun jar(): String {
        val header = runCatching { CookieManager.getInstance().getCookie(AmizoneClient.BASE) }.getOrNull() ?: return "(no cookies)"
        return header.split(';').map { it.trim() }.filter { it.contains('=') }
            .groupBy { it.substringBefore('=') }
            .entries.joinToString(" ") { (n, vs) -> n + "=" + vs.joinToString("|") { fp(it.substringAfter('=')) } + if (vs.size > 1) "×${vs.size}" else "" }
    }
}
