package com.student.amizoneplus.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.io.File
import java.net.URLDecoder

/**
 * Amizone LMS (Moodle at alms1.amizone.net/moodle_<semester>).
 *
 * Sign-in: Amizone's Home page has an "LMS" widget with one hidden form per
 * semester (usercode, token, userId) that POSTs to <moodle>/submit.php. Posting
 * it gives a Moodle session cookie — which, thanks to the shared cookie store,
 * also signs in every LMS page opened in the app's web view.
 *
 * Data: Moodle's standard AJAX API with the page's sesskey — the course list
 * and each course's sections exactly as the teacher arranged them.
 */
class Lms(private val amizone: AmizoneClient) {

    private data class Session(val wwwroot: String, val sesskey: String, val semester: String)

    private var session: Session? = null
    private val json = Json { ignoreUnknownKeys = true }

    private fun decodeHtml(s: String) =
        s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")

    /** Semester buttons in the Amizone "LMS" widget, newest first. */
    suspend fun semesters(): List<LmsSemester> {
        val home = amizone.home()
        val out = ArrayList<LmsSemester>()
        val btn = Regex("<(a|button)\\b([^>]*)>([\\s\\S]{0,200}?)</\\1>", RegexOption.IGNORE_CASE)
        for (m in btn.findAll(home)) {
            val label = m.groupValues[3].replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()
            if (!Regex("20\\d\\d-20\\d\\d\\s*/\\s*(odd|even)", RegexOption.IGNORE_CASE).containsMatchIn(label)) continue
            val formId = Regex("getElementById\\(['\"]([^'\"]+)['\"]\\)\\.submit").find(m.groupValues[2])?.groupValues?.get(1) ?: continue
            val at = maxOf(home.indexOf("id=\"$formId\""), home.indexOf("id='$formId'"))
            if (at < 0) continue
            val start = home.lastIndexOf("<form", at)
            val end = home.indexOf("</form>", at)
            if (start < 0 || end < start) continue
            val form = home.substring(start, end)
            val action = decodeHtml(Regex("action=[\"']([^\"']+)", RegexOption.IGNORE_CASE).find(form)?.groupValues?.get(1).orEmpty())
            val body = Regex("<input\\b([^>]*)>", RegexOption.IGNORE_CASE).findAll(form).mapNotNull { inp ->
                val name = Regex("name=[\"']([^\"']+)", RegexOption.IGNORE_CASE).find(inp.groupValues[1])?.groupValues?.get(1) ?: return@mapNotNull null
                val value = decodeHtml(Regex("value=[\"']([^\"']*)", RegexOption.IGNORE_CASE).find(inp.groupValues[1])?.groupValues?.get(1).orEmpty())
                java.net.URLEncoder.encode(name, "UTF-8") + "=" + java.net.URLEncoder.encode(value, "UTF-8")
            }.joinToString("&")
            if (action.isNotEmpty()) out += LmsSemester(label, action, body)
        }
        return out
    }

    private suspend fun signIn(sem: LmsSemester, retried: Boolean = false): Session {
        val found = withContext(Dispatchers.IO) {
            val url = if (sem.action.startsWith("http")) sem.action else AmizoneClient.BASE + sem.action
            val req = Request.Builder().url(url)
                .header("Referer", AmizoneClient.BASE + "/Home")
                .post(sem.body.toRequestBody("application/x-www-form-urlencoded".toMediaType())).build()
            val html = Http.client.newCall(req).execute().use { it.body?.string().orEmpty() }
            val wwwroot = Regex("\"wwwroot\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)?.replace("\\/", "/")
            val sesskey = Regex("\"sesskey\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            if (wwwroot != null && sesskey != null) Session(wwwroot, sesskey, sem.label) else null
        }
        if (found != null) return found.also { session = it }
        // The widget's sign-in tokens go stale: take fresh ones from Home and try once more.
        if (!retried) semesters().firstOrNull { it.label == sem.label }?.let { return signIn(it, true) }
        throw AmizoneException.Network("Couldn't sign in to the ${sem.label} LMS. It may be closed for that semester.")
    }

    private suspend fun ensure(sem: LmsSemester): Session =
        session?.takeIf { it.semester == sem.label } ?: signIn(sem)

    private suspend fun ajax(sem: LmsSemester, method: String, args: JsonObject, retried: Boolean = false): kotlinx.serialization.json.JsonElement {
        val s = ensure(sem)
        val payload = buildJsonArray { add(buildJsonObject { put("index", 0); put("methodname", method); put("args", args) }) }.toString()
        val text = withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("${s.wwwroot}/lib/ajax/service.php?sesskey=${s.sesskey}&info=$method")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            Http.client.newCall(req).execute().use { it.body?.string().orEmpty() }
        }
        val first = runCatching { json.parseToJsonElement(text).jsonArray[0].jsonObject }.getOrNull()
        val failed = first == null || (first["error"] as? JsonPrimitive)?.contentOrNull == "true"
        if (failed) {
            if (!retried) {
                session = null
                return ajax(sem, method, args, true)
            }
            throw AmizoneException.Network("LMS request failed.")
        }
        return first!!["data"]!!
    }

    suspend fun courses(sem: LmsSemester): List<LmsCourse> {
        val data = ajax(sem, "core_course_get_enrolled_courses_by_timeline_classification", buildJsonObject {
            put("offset", 0); put("limit", 0); put("classification", "all"); put("sort", "fullname")
        })
        val list = data.jsonObject["courses"]?.jsonArray ?: JsonArray(emptyList())
        return list.map { el ->
            val o = el.jsonObject
            val short = decodeHtml(o["shortname"]?.jsonPrimitive?.contentOrNull.orEmpty())
            val code = Regex("/\\s*([A-Z]{2,6}\\s?\\d{3}[A-Z]?)\\s*/").find(short)?.groupValues?.get(1)?.replace(" ", "").orEmpty()
            LmsCourse(o["id"]!!.jsonPrimitive.content, decodeHtml(o["fullname"]?.jsonPrimitive?.contentOrNull.orEmpty()), short, code)
        }
    }

    /** A course's sections and items in the teacher's own order. */
    suspend fun content(sem: LmsSemester, courseId: String): List<LmsSection> {
        val raw = ajax(sem, "core_courseformat_get_state", buildJsonObject { put("courseid", courseId.toInt()) })
        val state = (raw as? JsonPrimitive)?.contentOrNull?.let { json.parseToJsonElement(it) } ?: raw
        val obj = state.jsonObject
        val cms = obj["cm"]?.jsonArray.orEmpty().associateBy { it.jsonObject["id"]!!.jsonPrimitive.content }
        return obj["section"]?.jsonArray.orEmpty().map { s ->
            val so = s.jsonObject
            LmsSection(
                id = so["id"]!!.jsonPrimitive.content,
                title = decodeHtml(so["title"]?.jsonPrimitive?.contentOrNull.orEmpty()),
                items = so["cmlist"]?.jsonArray.orEmpty().mapNotNull { id ->
                    val cm = cms[id.jsonPrimitive.content]?.jsonObject ?: return@mapNotNull null
                    if (cm["uservisible"]?.jsonPrimitive?.contentOrNull == "false") return@mapNotNull null
                    LmsItem(
                        id = cm["id"]!!.jsonPrimitive.content,
                        name = decodeHtml(cm["name"]?.jsonPrimitive?.contentOrNull.orEmpty()),
                        // "module" is the internal type (folder, resource, url…); "modname" is only the
                        // translated display name ("Folder", "File"), so it must not be used for logic.
                        modname = (cm["module"]?.jsonPrimitive?.contentOrNull ?: cm["modname"]?.jsonPrimitive?.contentOrNull.orEmpty()).lowercase(),
                        url = cm["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    )
                },
            )
        }
    }

    /** Files inside a Moodle folder activity. */
    suspend fun folderFiles(sem: LmsSemester, item: LmsItem): List<LmsFile> {
        val s = ensure(sem)
        val html = withContext(Dispatchers.IO) {
            val url = item.url.ifEmpty { "${s.wwwroot}/mod/folder/view.php?id=${item.id}" }
            Http.client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string().orEmpty() }
        }
        val doc = Jsoup.parse(html)
        val seen = HashSet<String>()
        return doc.select("a[href*=pluginfile.php]").mapNotNull { a ->
            val href = a.attr("href")
            if (!seen.add(href)) return@mapNotNull null
            val fromUrl = URLDecoder.decode(href.substringBefore('?').substringAfterLast('/'), "UTF-8")
            val name = (a.selectFirst(".fp-filename")?.text() ?: a.text()).trim().ifEmpty { fromUrl }
            val sub = href.substringBefore('?').substringAfter("/mod_folder/content/", "").split('/').drop(1).dropLast(1)
            LmsFile(name, href, sub.joinToString(" / ") { URLDecoder.decode(it, "UTF-8") })
        }
    }

    sealed interface OpenResult {
        data object Opened : OpenResult
        data class Web(val url: String) : OpenResult
    }

    /** Downloads with the LMS session and opens it in a viewer app. Web pages come back as [OpenResult.Web]. */
    suspend fun open(context: Context, sem: LmsSemester, url: String, nameHint: String): OpenResult {
        val s = ensure(sem)
        return when (val r = Downloads.open(context, url, nameHint, referer = s.wwwroot + "/")) {
            is Downloads.Result.Web -> OpenResult.Web(r.url)
            Downloads.Result.Opened -> OpenResult.Opened
        }
    }

    /** Resource / link items open their target directly. */
    fun itemUrl(item: LmsItem) =
        if (item.modname == "resource" || item.modname == "url") item.url + (if (item.url.contains('?')) "&" else "?") + "redirect=1" else item.url

    companion object {
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
    }
}
