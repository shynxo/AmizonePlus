package com.student.amizoneplus.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.student.amizoneplus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File

/**
 * In-app updates from GitHub Releases.
 *
 * Publishing an update = a GitHub Release tagged like "v2.2.0" with the signed APK attached.
 * The app reads the latest release, compares it with its own version, and offers it; the
 * APK is downloaded into the cache and handed to Android's installer (the user confirms).
 * Android only accepts it when it is signed with the same key as the installed app.
 */
object Updater {
    data class Release(val version: String, val tag: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

    private val json = Json { ignoreUnknownKeys = true }
    private const val PREFS = "updater"

    val repo: String get() = BuildConfig.UPDATE_REPO
    val enabled: Boolean get() = repo.contains('/') && !repo.startsWith("YOUR_")
    val current: String get() = BuildConfig.VERSION_NAME

    /** "v2.10.1" > "2.9" — numeric, part by part. */
    fun newer(candidate: String, than: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").split('.', '-', '+')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parts(candidate); val b = parts(than)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Latest release if it's newer than this app, else null. Throws on network trouble. */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        val req = Request.Builder().url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json").build()
        val text = Http.client.newCall(req).execute().use { r ->
            if (r.code == 404) return@withContext null // no releases yet
            if (!r.isSuccessful) throw AmizoneException.Network("Couldn't check for updates (HTTP ${r.code}).")
            r.body?.string().orEmpty()
        }
        val o = json.parseToJsonElement(text).jsonObject
        val tag = o["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@withContext null
        val apk = o["assets"]?.jsonArray.orEmpty().map { it.jsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull.orEmpty().endsWith(".apk", true) } ?: return@withContext null
        val rel = Release(
            version = tag.removePrefix("v").removePrefix("V"),
            tag = tag,
            notes = plain(o["body"]?.jsonPrimitive?.contentOrNull.orEmpty()),
            apkUrl = apk["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@withContext null,
            sizeBytes = apk["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
        )
        if (newer(rel.version, current)) rel else null
    }

    /** Release notes are Markdown; show them as plain text with bullets. */
    private fun plain(md: String): String = md.replace("\r", "").lines().joinToString("\n") { line ->
        val l = line.trimEnd()
        when {
            l.trimStart().startsWith("#") -> l.trimStart().trimStart('#').trim().uppercase()
            Regex("^\\s*[*\\-+] ").containsMatchIn(l) -> "• " + l.trimStart().drop(2)
            else -> l
        }.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1").replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
    }.trim()

    /** Check at most every few hours on launch; "Later" hides a version for a day. */
    fun shouldAutoCheck(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return System.currentTimeMillis() - p.getLong("lastCheck", 0) > 4 * 3_600_000L
    }

    fun markChecked(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("lastCheck", System.currentTimeMillis()).apply()

    fun snoozed(context: Context, version: String): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString("snoozedVersion", null) == version && System.currentTimeMillis() < p.getLong("snoozedUntil", 0)
    }

    fun snooze(context: Context, version: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("snoozedVersion", version).putLong("snoozedUntil", System.currentTimeMillis() + 86_400_000L).apply()

    /** Downloads the APK into the cache, reporting progress 0..1. */
    suspend fun download(context: Context, rel: Release, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, "amizoneplus-${rel.version}.apk")
        Http.client.newCall(Request.Builder().url(rel.apkUrl).build()).execute().use { r ->
            if (!r.isSuccessful) throw AmizoneException.Network("Download failed (HTTP ${r.code}).")
            val body = r.body ?: throw AmizoneException.Network("Download failed.")
            val total = body.contentLength().takeIf { it > 0 } ?: rel.sizeBytes
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) progress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        out
    }

    /** Android needs a one-time "allow installs from this app" for updates. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Hands the APK to Android's installer; the user taps Install. Data and login are kept. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
