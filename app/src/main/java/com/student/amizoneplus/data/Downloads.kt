package com.student.amizoneplus.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.net.URLDecoder

/**
 * Downloads a file with the app's signed-in connection (shared cookies) into the
 * cache and opens it in the phone's viewer app (PDF, PowerPoint, Word…).
 */
object Downloads {
    sealed interface Result {
        data object Opened : Result
        data class Web(val url: String) : Result // turned out to be a web page
    }

    val MIME = mapOf(
        "pdf" to "application/pdf",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "zip" to "application/zip",
        "txt" to "text/plain",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "mp4" to "video/mp4",
    )

    private data class Got(val bytes: ByteArray?, val name: String, val mime: String, val finalUrl: String, val html: Boolean)

    suspend fun open(context: Context, url: String, nameHint: String, referer: String? = null): Result {
        val got = withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).apply { if (referer != null) header("Referer", referer) }.build()
            Http.client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw AmizoneException.Network("Download failed (HTTP ${r.code}).")
                val type = r.header("Content-Type").orEmpty().substringBefore(';').trim()
                if (type == "text/html") return@use Got(null, "", type, r.request.url.toString(), true)
                val disp = r.header("Content-Disposition").orEmpty()
                val fromHeader = Regex("filename\\*?=(?:UTF-8'')?[\"']?([^\"';]+)", RegexOption.IGNORE_CASE).find(disp)?.groupValues?.get(1)
                    ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
                val fromUrl = runCatching { URLDecoder.decode(r.request.url.encodedPath.substringAfterLast('/'), "UTF-8") }.getOrDefault("")
                var n = (fromHeader ?: fromUrl.takeIf { it.contains('.') } ?: URLUtil.guessFileName(url, disp, type).takeIf { it.contains('.') } ?: nameHint)
                    .replace(Regex("[\\\\/:*?\"<>|]+"), "_").take(120)
                if (!n.contains('.')) MIME.entries.firstOrNull { it.value == type }?.let { n = "$n.${it.key}" }
                Got(r.body?.bytes(), n, type, r.request.url.toString(), false)
            }
        }
        if (got.html || got.bytes == null) return Result.Web(got.finalUrl)

        val dir = File(context.cacheDir, "lms").apply { mkdirs() }
        val file = File(dir, got.name).apply { writeBytes(got.bytes) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val type = MIME[got.name.substringAfterLast('.', "").lowercase()] ?: got.mime.ifEmpty { "*/*" }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: ActivityNotFoundException) {
            val share = Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(share, got.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return Result.Opened
    }
}
