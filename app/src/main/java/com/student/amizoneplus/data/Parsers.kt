package com.student.amizoneplus.data

import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Calendar

/**
 * HTML/JSON parsers for Amizone pages. Selectors follow the open-source go-amizone
 * project and were verified against your live account. If Amizone changes a page,
 * this is the file to update.
 */
object Parsers {

    private fun clean(s: String?) = (s ?: "").replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim()
    private fun cell(row: Element, title: String): Element? = row.selectFirst("td[data-title=\"$title\"]")
    private fun cellText(row: Element, title: String) = clean(cell(row, title)?.text())
    private val BR = Regex("<br\\b[^>]*>", RegexOption.IGNORE_CASE)
    private fun htmlText(s: String) = clean(Jsoup.parse(s).text())

    /* ---------- Login ---------- */

    fun isLoginPage(html: String) =
        Regex("id\\s*=\\s*[\"']loginform[\"']", RegexOption.IGNORE_CASE).containsMatchIn(html) ||
            Regex("name\\s*=\\s*[\"']_Password[\"']", RegexOption.IGNORE_CASE).containsMatchIn(html)

    fun mentionsBadCredentials(html: String) =
        Regex("invalid (user ?name|password|credentials)|incorrect (user ?name|password)|wrong password", RegexOption.IGNORE_CASE)
            .containsMatchIn(html)

    /** Every named field of Amizone's login form, with the values the page set. */
    fun loginForm(html: String): MutableMap<String, String>? {
        val doc = org.jsoup.Jsoup.parse(html)
        val form = doc.selectFirst("form#loginform") ?: doc.selectFirst("form:has(input[name=_Password])") ?: return null
        val out = LinkedHashMap<String, String>()
        for (inp in form.select("input[name]")) {
            val name = inp.attr("name")
            if (name.isEmpty() || name in out) continue
            out[name] = inp.attr("value")
        }
        return if ("__RequestVerificationToken" in out) out else null
    }

    fun verificationToken(html: String): String? =
        Jsoup.parse(html).selectFirst("input[name=__RequestVerificationToken]")?.attr("value")?.takeIf { it.isNotEmpty() }

    /* ---------- Attendance (home page widget) ---------- */

    fun attendance(html: String): List<AttendanceRecord> {
        val doc = Jsoup.parse(html)
        val header = doc.select(".widget-header").firstOrNull { it.text().contains("my attendance", ignoreCase = true) }
        val items = (header?.parent() ?: doc).select("ul#tasks li")
        if (items.isEmpty()) throw AmizoneException.Parse("Attendance widget not found")
        return items.map { li ->
            val nums = clean(li.selectFirst("div.class-count span")?.text()).replace("\"", "").split("/")
            val attended = nums.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val held = nums.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val code = clean(li.selectFirst("span.sub-code")?.text())
            val label = clean(li.selectFirst("span.lbl")?.text())
            val name = if (label.startsWith(code)) clean(label.removePrefix(code)) else label.replaceFirst(Regex("^\\S+\\s"), "")
            AttendanceRecord(code, name.ifEmpty { code }, attended, held)
        }
    }

    /* ---------- Class schedule (calendar JSON) ---------- */

    /** Accepts "2024/01/15 09:15:00 AM" and "2024-01-15 09:15:00" / ISO formats. Local time. */
    private fun diaryTime(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        val m = Regex("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})[ T](\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\s*(AM|PM)?", RegexOption.IGNORE_CASE)
            .find(s) ?: return null
        val g = m.groupValues
        var h = g[4].toInt()
        val ampm = g[7].uppercase()
        if (ampm == "PM" && h < 12) h += 12
        if (ampm == "AM" && h == 12) h = 0
        return Calendar.getInstance().apply {
            set(g[1].toInt(), g[2].toInt() - 1, g[3].toInt(), h, g[5].toInt(), g[6].ifEmpty { "0" }.toInt())
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun attendanceState(color: String?) = when ((color ?: "").uppercase()) {
        "#F00", "#FF0000" -> "absent"
        "#4FCC4F" -> "present"
        "#3A87AD" -> "pending"
        else -> "na"
    }

    fun schedule(json: String): List<ScheduledClass> {
        val arr = try {
            JSONArray(json)
        } catch (e: Exception) {
            throw AmizoneException.Parse("Timetable response was not JSON")
        }
        val out = ArrayList<ScheduledClass>()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            if (e.optString("sType") != "C") continue
            val start = diaryTime(e.optString("start")) ?: continue
            val end = diaryTime(e.optString("end")) ?: start
            out += ScheduledClass(
                code = htmlText(e.optString("CourseCode")),
                name = htmlText(e.optString("title")),
                startMs = start,
                endMs = end,
                faculty = htmlText(e.optString("FacultyName")),
                room = htmlText(e.optString("RoomNo")),
                state = attendanceState(e.optString("AttndColor")),
            )
        }
        return out.sortedBy { it.startMs }
    }

    /* ---------- Courses (My Courses page) ---------- */

    private fun absolute(href: String, base: String) =
        if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(href)) href
        else base + (if (href.startsWith("/")) "" else "/") + href

    fun courses(html: String, base: String): List<Course> {
        val doc = Jsoup.parse(html)
        val rows = doc.select("tr").filter { cell(it, "Course Code") != null }
        if (rows.isEmpty()) throw AmizoneException.Parse("No courses found")
        val seen = HashSet<String>()
        val out = ArrayList<Course>()
        for (row in rows) {
            val code = cellText(row, "Course Code")
            if (code.isEmpty() || !seen.add(code)) continue
            val att = Regex("(\\d+)\\s*/\\s*(\\d+)").find(cellText(row, "Attendance"))
            // e.g. "35.00[30.00+5.00]/40.00" or "12/30"
            val marksRaw = cellText(row, "Internal Asses.")
            val have = Regex("^(\\d+(?:\\.\\d+)?)").find(marksRaw)?.groupValues?.get(1)?.toDoubleOrNull()
            val max = Regex("/\\s*(\\d+(?:\\.\\d+)?)\\s*$").find(marksRaw)?.groupValues?.get(1)?.toDoubleOrNull()
            val syllabus = cell(row, "Course Syllabus")?.selectFirst("a")?.attr("href").orEmpty()
            out += Course(
                code = code,
                name = cellText(row, "Course Name"),
                type = cellText(row, "Type"),
                attended = att?.groupValues?.get(1)?.toInt() ?: 0,
                held = att?.groupValues?.get(2)?.toInt() ?: 0,
                internals = if (have != null && max != null) Internals(have, max) else null,
                syllabusUrl = syllabus.takeIf { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("javascript:", true) }
                    ?.let { absolute(it, base) },
            )
        }
        return out
    }

    /* ---------- Exam schedule ---------- */

    fun examSchedule(html: String): ExamSchedule {
        val doc = Jsoup.parse(html)
        val rows = doc.select("table.table tbody tr").filter { cell(it, "Course Code") != null }
        val exams = rows.map { row ->
            val d = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})").find(cellText(row, "Exam Date"))
            val t = Regex("(\\d{1,2}):(\\d{2})").find(cellText(row, "Time"))
            val time = d?.let {
                Calendar.getInstance().apply {
                    set(it.groupValues[3].toInt(), it.groupValues[2].toInt() - 1, it.groupValues[1].toInt(),
                        t?.groupValues?.get(1)?.toInt() ?: 9, t?.groupValues?.get(2)?.toInt() ?: 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            val typeCell = cell(row, "Paper Type")
            val modeRaw = clean(typeCell?.selectFirst("b")?.text())
            val mode = if (modeRaw.contains(':')) clean(modeRaw.substringAfter(':')) else modeRaw
            var location = ""
            val red = typeCell?.select("b")?.firstOrNull { Regex("color\\s*:\\s*red", RegexOption.IGNORE_CASE).containsMatchIn(it.attr("style")) }
            if (red != null) {
                val lines = red.html().split(BR).map { htmlText(it) }
                val first = lines.firstOrNull { it.contains(':') } ?: lines.firstOrNull().orEmpty()
                location = clean(if (first.contains(':')) first.substringAfter(':') else first).trim('-', ' ')
            }
            ScheduledExam(cellText(row, "Course Code"), cellText(row, "Course Title"), time, mode, location)
        }
        val title = clean(doc.selectFirst("div.page-header h1")?.text()).ifEmpty { "Examination Schedule" }
        return ExamSchedule(title, exams.sortedBy { it.timeMs ?: Long.MAX_VALUE })
    }

    /* ---------- Profile ---------- */

    fun profile(html: String): Profile {
        val doc = Jsoup.parse(html)
        val front = doc.selectFirst("#lblNameIDCardFront1") ?: throw AmizoneException.Parse("ID card not found")
        val parts = front.html().split(BR).map { htmlText(it) }.filter { it.isNotEmpty() }
        val backText = clean(doc.selectFirst("#lblInfoIDCardBack1")?.text() ?: html)
        val enrollment = Regex("enrol+ment\\s*no\\.?\\s*:\\s*([A-Za-z0-9]+)", RegexOption.IGNORE_CASE).find(backText)?.groupValues?.get(1).orEmpty()
        return Profile(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" }, enrollment)
    }

    /** Name from the header of any logged-in page, e.g. <span class="user-info"><small>Welcome,</small> JOHN DOE</span>. */
    fun headerName(html: String): String {
        val info = Jsoup.parse(html).selectFirst(".user-info") ?: return ""
        info.select("small").remove()
        val name = clean(info.text())
            .replaceFirst(Regex("^(welcome|hi|hello)\\s*,?\\s*", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(mr|mrs|ms|miss|dr)\\.?\\s+", RegexOption.IGNORE_CASE), "")
        if (name != name.uppercase()) return name
        return name.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }

    /* ---------- Results ---------- */

    private fun num(s: String) = s.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0

    fun results(html: String): Results {
        val doc = Jsoup.parse(html)
        val courseRows = doc.select("tr").filter { cell(it, "Course Code") != null && cell(it, "Go") != null }
        val semRows = doc.select("tr").filter { cell(it, "SGPA") != null }
        if (courseRows.isEmpty() && semRows.isEmpty()) throw AmizoneException.Parse("No results found")
        return Results(
            courses = courseRows.map {
                CourseResult(cellText(it, "Course Code"), cellText(it, "Course Title"), cellText(it, "Go"),
                    num(cellText(it, "GP")), num(cellText(it, "ACU")), num(cellText(it, "Max Total")))
            },
            semesters = semRows.map {
                SemesterResult(cellText(it, "Semester"), num(cellText(it, "SGPA")), num(cellText(it, "CGPA")), cellText(it, "Back Papers"))
            },
        )
    }

    /* ---------- Document downloads ---------- */

    fun noticeDocs(html: String): List<NoticeDoc> =
        Jsoup.parse(html).select("tr").filter { cell(it, "Title") != null }.map {
            NoticeDoc(cellText(it, "Title"), cellText(it, "Description"), cell(it, "Downloads")?.selectFirst("a")?.attr("href").orEmpty())
        }.filter { it.url.startsWith("http", ignoreCase = true) }

    /* ---------- Programme structure → credits per course code ---------- */

    /**
     * Teaching hours from the Programme Structure, where every course reads like
     * "MATH211 - Applied Mathematics- III [ CU- 4.00 , L- 3.00 , T- 0.00 , P- 0.00 , … ]".
     */
    fun structure(html: String): Map<String, CourseHours> {
        val text = Jsoup.parse(html).text()
        val re = Regex(
            "([A-Z]{2,6}\\s?\\d{3}[A-Z]?)\\s*-[^\\[]{0,160}\\[\\s*CU\\s*-\\s*([\\d.]+)\\s*,\\s*L\\s*-\\s*([\\d.]+)\\s*,\\s*T\\s*-\\s*([\\d.]+)\\s*,\\s*P\\s*-\\s*([\\d.]+)",
            RegexOption.IGNORE_CASE,
        )
        val out = LinkedHashMap<String, CourseHours>()
        for (m in re.findAll(text)) {
            val code = m.groupValues[1].replace(" ", "").uppercase()
            val h = CourseHours(
                m.groupValues[2].toDoubleOrNull() ?: 0.0, m.groupValues[3].toDoubleOrNull() ?: 0.0,
                m.groupValues[4].toDoubleOrNull() ?: 0.0, m.groupValues[5].toDoubleOrNull() ?: 0.0,
            )
            if (h.cu > 0 || h.weekly > 0) out[code] = h
        }
        return out
    }

    fun credits(html: String): Map<String, Double> {
        val doc: Document = Jsoup.parse(html)
        val out = HashMap<String, Double>()
        val codeRe = Regex("^[A-Z]{2,6}\\s?\\d{3}[A-Z]?$")
        for (table in doc.select("table")) {
            val headers = (table.select("thead th").ifEmpty { table.selectFirst("tr")?.select("th,td") ?: emptyList() })
                .map { clean(it.text()).lowercase() }
            val codeIdx = headers.indexOfFirst { it.contains("code") }
            val creditIdx = headers.indexOfFirst { it.contains("credit") }
            for (row in table.select("tr")) {
                var code = clean(row.selectFirst("td[data-title*=Code]")?.text())
                var credit = clean(row.selectFirst("td[data-title*=Credit], td[data-title*=credit]")?.text()).toDoubleOrNull()
                if ((code.isEmpty() || credit == null) && codeIdx >= 0 && creditIdx >= 0) {
                    val cells = row.select("td")
                    if (cells.size > maxOf(codeIdx, creditIdx)) {
                        code = clean(cells[codeIdx].text())
                        credit = clean(cells[creditIdx].text()).toDoubleOrNull()
                    }
                }
                code = code.replace(" ", "").uppercase()
                if (codeRe.matches(code) && credit != null && credit > 0 && credit <= 30) out[code] = credit
            }
        }
        return out
    }
}
