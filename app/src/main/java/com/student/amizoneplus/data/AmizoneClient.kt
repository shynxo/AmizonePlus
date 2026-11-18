package com.student.amizoneplus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Talks to s.amizone.net exactly like a browser: a GET for the login page's
 * anti-forgery token, then a form POST with your ID/password. Every request
 * re-signs in automatically when the session has expired.
 */
class AmizoneClient(private val credentials: () -> Credentials?) {

    companion object {
        const val BASE = "https://s.amizone.net"
    }

    data class Page(val text: String, val url: String, val code: Int)

    private suspend fun raw(path: String, form: Map<String, String>? = null, ajax: Boolean = false): Page = withContext(Dispatchers.IO) {
        val url = if (path.startsWith("http")) path else BASE + path
        val b = Request.Builder().url(url).header("Referer", "$BASE/")
        if (ajax) b.header("X-Requested-With", "XMLHttpRequest")
        if (form != null) b.post(FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build())
        try {
            Http.client.newCall(b.build()).execute().use { r ->
                if (r.code >= 500) throw AmizoneException.Network("Amizone returned ${r.code}. It may be down.")
                Page(r.body?.string().orEmpty(), r.request.url.toString(), r.code)
            }
        } catch (e: IOException) {
            throw AmizoneException.Network("Couldn't reach Amizone. Check your internet connection.")
        }
    }

    // Only one sign-in at a time: a refresh loads many pages in parallel, and several
    // simultaneous sign-ins overwrite each other's anti-forgery cookie and all fail.
    private val loginLock = Mutex()
    @Volatile private var lastLogin = 0L

    /** Signs in with the given (or saved) credentials. Returns when a session exists. */
    suspend fun login(creds: Credentials? = null): Unit = loginLock.withLock {
        // Another request just signed in while this one waited: nothing to do.
        if (creds == null && System.currentTimeMillis() - lastLogin < 15_000) return@withLock
        val c = creds ?: credentials() ?: throw AmizoneException.InvalidCredentials("No saved login.")
        val loginPage = raw("/")
        if (!Parsers.isLoginPage(loginPage.text)) { lastLogin = System.currentTimeMillis(); return@withLock } // already signed in
        // Send the form exactly as the page would: every hidden field it carries
        // (anti-forgery token, Salt/Challenge/Signature, honeypot…) plus your details.
        val form = Parsers.loginForm(loginPage.text)
            ?: throw AmizoneException.NeedsWebLogin("The Amizone login page has changed. Please sign in through the website view.")
        form["_UserName"] = c.username
        form["_Password"] = c.password
        val res = raw("/", form)
        if (Parsers.isLoginPage(res.text)) {
            if (Parsers.mentionsBadCredentials(res.text)) throw AmizoneException.InvalidCredentials()
            throw AmizoneException.NeedsWebLogin("Amizone wants a quick human check. Tap Sign in, tick the box and log in once.")
        }
        lastLogin = System.currentTimeMillis()
        Http.persistSession()
    }

    /** GET a page, signing in again if the session expired. */
    suspend fun get(path: String, ajax: Boolean = false): String {
        var res = raw(path, ajax = ajax)
        if (Parsers.isLoginPage(res.text)) {
            login()
            res = raw(path, ajax = ajax)
            if (Parsers.isLoginPage(res.text)) throw AmizoneException.NeedsWebLogin("Could not stay signed in to Amizone.")
        }
        return res.text
    }

    suspend fun home(): String = get("/Home")

    /**
     * A tiny signed-in request so the Amizone session doesn't expire from inactivity.
     * Never signs in by itself (that's left to the app), so it can't trip the human check.
     */
    suspend fun keepAlive(): Boolean {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val res = raw("/Calendar/home/GetDiaryEvents?start=$d&end=$d", ajax = true)
        val ok = !Parsers.isLoginPage(res.text)
        if (ok) Http.persistSession()
        return ok
    }

    suspend fun schedule(fromMs: Long, days: Int): List<ScheduledClass> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val start = Calendar.getInstance().apply {
            timeInMillis = fromMs; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, days) }
        return Parsers.schedule(get("/Calendar/home/GetDiaryEvents?start=${fmt.format(start.time)}&end=${fmt.format(end.time)}", ajax = true))
    }

    /**
     * Weekly L / T / P for your courses. Amizone's Programme Structure page is only a
     * semester picker; each semester's courses come from BindProgramList. Start at the
     * likely current semester and widen until every current course is found.
     */
    private suspend fun programmeHours(codes: List<String>, guess: Int): Map<String, CourseHours> {
        val want = codes.map { it.replace(" ", "").uppercase() }.toSet()
        val start = guess.coerceIn(1, 8)
        val order = (1..8).sortedBy { kotlin.math.abs(it - start) * 2 + (if (it < start) 1 else 0) }
        val out = HashMap<String, CourseHours>()
        for (sem in order) {
            if (want.isNotEmpty() && want.all { it in out }) break
            val found = runCatching { Parsers.structure(get("/Academics/ProgrammeStructure/BindProgramList?iSemester=$sem", ajax = true)) }.getOrDefault(emptyMap())
            found.forEach { (k, v) -> out.putIfAbsent(k, v) }
            if (want.isEmpty() && found.isNotEmpty()) break
        }
        return out
    }

    /** Fetches everything; sections that fail are listed in `errors` instead of failing the refresh. */
    suspend fun fetchAll(): AmizoneData = coroutineScope {
        val home = home()
        val errors = mutableListOf<String>()

        suspend fun <T> attempt(label: String, fallback: T, block: suspend () -> T): T = try {
            block()
        } catch (e: AmizoneException.Parse) {
            synchronized(errors) { errors += label }
            fallback
        } catch (e: AmizoneException.Network) {
            if (label in setOf("History", "Documents", "Credits", "Results")) {
                synchronized(errors) { errors += label }; fallback
            } else throw e
        }

        val now = System.currentTimeMillis()
        val courses = async { attempt("Courses", emptyList()) { Parsers.courses(get("/Academics/MyCourses"), BASE) } }
        val schedule = async { attempt("Timetable", emptyList()) { schedule(now, 8) } }
        val exams = async { attempt<ExamSchedule?>("Exam schedule", null) { Parsers.examSchedule(get("/Examination/ExamSchedule")) } }
        val profile = async {
            attempt<Profile?>("Profile", null) {
                try {
                    Parsers.profile(get("/IDCard"))
                } catch (e: AmizoneException.Parse) {
                    val name = Parsers.headerName(home)
                    if (name.isEmpty()) throw e
                    Profile(name, "", "", "")
                }
            }
        }
        val history = async { attempt("History", emptyList()) { schedule(now - 28L * 86_400_000, 28) } }
        val results = async { attempt<Results?>("Results", null) { Parsers.results(get("/Examination/Examination")) } }
        val notices = async { attempt("Documents", emptyList()) { Parsers.noticeDocs(get("/StudentDocument/StudentDocument")) } }

        val c = courses.await()
        var attendance = attempt("Attendance", emptyList()) { Parsers.attendance(home) }
        if (attendance.isEmpty() && c.isNotEmpty()) {
            attendance = c.filter { it.held > 0 }.map { AttendanceRecord(it.code, it.name, it.attended, it.held) }
            errors.remove("Attendance")
        }
        val r = results.await()
        val hours = programmeHours(c.map { it.code }, (r?.semesters?.size ?: 0) + 1)
        val creditMap = (r?.courses.orEmpty().filter { it.credits > 0 }.associate { it.code to it.credits }) +
            hours.filterValues { it.cu > 0 }.mapValues { it.value.cu }

        val quiet = setOf("History", "Documents", "Credits")
        AmizoneData(
            emailPassword = runCatching { Parsers.homeEmailPassword(home) }.getOrDefault(""),
            profile = profile.await()?.let { if (it.email.isEmpty()) it.copy(email = runCatching { Parsers.homeEmail(home) }.getOrDefault("")) else it },
            attendance = attendance,
            schedule = schedule.await(),
            history = history.await(),
            exams = exams.await(),
            courses = c,
            results = r,
            notices = notices.await(),
            credits = creditMap,
            hours = hours,
            fetchedAt = System.currentTimeMillis(),
            errors = errors.filterNot { it in quiet },
        )
    }
}
