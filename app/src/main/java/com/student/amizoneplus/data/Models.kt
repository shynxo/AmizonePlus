package com.student.amizoneplus.data

import kotlinx.serialization.Serializable

/** Everything the app shows, cached as JSON so it opens instantly and works offline. */

@Serializable
data class AttendanceRecord(val code: String, val name: String, val attended: Int, val held: Int)

/** present / absent / pending / na */
@Serializable
data class ScheduledClass(
    val code: String,
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val faculty: String,
    val room: String,
    val state: String,
)

@Serializable
data class ScheduledExam(
    val code: String,
    val name: String,
    val timeMs: Long?,
    val mode: String,
    val location: String,
)

@Serializable
data class ExamSchedule(val title: String, val exams: List<ScheduledExam>)

@Serializable
data class Internals(val have: Double, val max: Double)

@Serializable
data class Course(
    val code: String,
    val name: String,
    val type: String,
    val attended: Int,
    val held: Int,
    val internals: Internals?,
    val syllabusUrl: String?,
)

@Serializable
data class Profile(val name: String, val program: String, val batch: String, val enrollmentNo: String)

@Serializable
data class CourseResult(val code: String, val name: String, val grade: String, val gradePoint: Double, val credits: Double, val max: Double)

@Serializable
data class SemesterResult(val semester: String, val sgpa: Double, val cgpa: Double, val backPapers: String)

@Serializable
data class Results(val courses: List<CourseResult>, val semesters: List<SemesterResult>)

@Serializable
data class NoticeDoc(val title: String, val description: String, val url: String)

@Serializable
data class AmizoneData(
    val profile: Profile? = null,
    val attendance: List<AttendanceRecord> = emptyList(),
    val schedule: List<ScheduledClass> = emptyList(), // today + next 7 days
    val history: List<ScheduledClass> = emptyList(), // last 28 days
    val exams: ExamSchedule? = null,
    val courses: List<Course> = emptyList(),
    val results: Results? = null,
    val notices: List<NoticeDoc> = emptyList(),
    val credits: Map<String, Double> = emptyMap(), // course code -> credit units
    val hours: Map<String, CourseHours> = emptyMap(), // course code -> weekly L / T / P from Programme Structure
    val fetchedAt: Long = 0,
    val errors: List<String> = emptyList(),
)

/** Weekly teaching hours from the Programme Structure: lectures, tutorials, practicals. */
@Serializable
data class CourseHours(val cu: Double = 0.0, val l: Double = 0.0, val t: Double = 0.0, val p: Double = 0.0) {
    /**
     * Classes per week that count for attendance: L + T + P. A course with no
     * lectures (languages, etc.) counts its tutorials at half: T 1 → 0.5 a week.
     */
    val weekly: Double get() = l + p + (if (l == 0.0) t / 2 else t)
}

@Serializable
data class Settings(
    val threshold: Int = 75,
    val lowAttendanceAlerts: Boolean = true,
    val classReminders: Boolean = true,
    val reminderMinutes: Int = 10,
    val examReminders: Boolean = true,
    val haptics: Boolean = true,
    val creditOverrides: Map<String, Double> = emptyMap(), // old (credits-based) — no longer used
    val weeklyOverrides: Map<String, Double> = emptyMap(), // course code -> classes per week, set by you
    val pets: String = "both", // "cat", "dog", "both" or "none"
    val menuStyle: String = "fade", // quick menu: "fade" (unblur in place) or "slide" (springs down)
)

data class Credentials(val username: String, val password: String)

/* ── LMS (Moodle) ── */

@Serializable
data class LmsSemester(val label: String, val action: String, val body: String)

@Serializable
data class LmsCourse(val id: String, val fullname: String, val shortname: String, val code: String)

@Serializable
data class LmsItem(val id: String, val name: String, val modname: String, val url: String)

@Serializable
data class LmsSection(val id: String, val title: String, val items: List<LmsItem>)

@Serializable
data class LmsFile(val name: String, val url: String, val path: String)

/* ── Errors ── */

sealed class AmizoneException(message: String) : Exception(message) {
    class InvalidCredentials(message: String = "Wrong username or password.") : AmizoneException(message)
    class NeedsWebLogin(message: String) : AmizoneException(message)
    class Network(message: String) : AmizoneException(message)
    class Parse(message: String) : AmizoneException(message)
}
