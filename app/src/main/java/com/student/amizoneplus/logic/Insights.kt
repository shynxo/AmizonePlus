package com.student.amizoneplus.logic

import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.ScheduledClass
import java.util.Calendar

private fun ScheduledClass.marked() = state == "present" || state == "absent"

/** Attendance rate per weekday (Mon..Sun); null when no marked classes that day. */
fun weekdayRates(history: List<ScheduledClass>, courseCode: String? = null): List<Double?> {
    val p = IntArray(7)
    val t = IntArray(7)
    for (c in history) {
        if (!c.marked() || (courseCode != null && c.code != courseCode)) continue
        val dow = Calendar.getInstance().apply { timeInMillis = c.startMs }.get(Calendar.DAY_OF_WEEK)
        val d = (dow + 5) % 7 // Mon=0
        t[d]++
        if (c.state == "present") p[d]++
    }
    return (0 until 7).map { if (t[it] > 0) p[it].toDouble() / t[it] else null }
}

/** One day of the current week, as the Today screen's circles show it. */
data class WeekDay(val label: String, val epochDay: Long, val kind: Kind, val present: Int, val marked: Int, val total: Int) {
    enum class Kind { Rate, Free, Pending, Future }
    val rate: Double get() = if (marked > 0) present.toDouble() / marked else 0.0
}

/**
 * Monday..Sunday of this week from the real classes on each date:
 * a percentage only for days whose classes were actually marked, [WeekDay.Kind.Free]
 * for past days with no classes at all (no fake 100%), and [WeekDay.Kind.Future] ahead.
 */
fun currentWeek(history: List<ScheduledClass>, schedule: List<ScheduledClass>, now: Long = System.currentTimeMillis()): List<WeekDay> {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val byDay = (history + schedule)
        .distinctBy { it.code + "@" + it.startMs }
        .groupBy { java.time.Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate() }
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return (0 until 7).map { i ->
        val d = monday.plusDays(i.toLong())
        val cs = byDay[d].orEmpty()
        val marked = cs.count { it.marked() }
        val present = cs.count { it.state == "present" }
        val kind = when {
            d.isAfter(today) -> WeekDay.Kind.Future
            cs.isEmpty() -> WeekDay.Kind.Free
            marked == 0 -> WeekDay.Kind.Pending
            else -> WeekDay.Kind.Rate
        }
        WeekDay(labels[i], d.toEpochDay(), kind, present, marked, cs.size)
    }
}

/** Consecutive "present" classes, counting back from the most recent marked class. */
fun presentStreak(history: List<ScheduledClass>): Int =
    history.filter { it.marked() }.sortedByDescending { it.startMs }.takeWhile { it.state == "present" }.size

data class Badge(val id: String, val short: String, val tag: String, val title: String, val detail: String)

fun earnedBadges(data: AmizoneData, threshold: Int): List<Badge> {
    val out = mutableListOf<Badge>()
    val att = data.attendance.filter { it.held > 0 }
    if (att.isNotEmpty() && att.all { percent(it.attended, it.held) >= threshold }) {
        out += Badge("all-above-$threshold", "$threshold%", "Steady", "All subjects on track", "Every subject is at or above $threshold%.")
    }
    val weekAgo = System.currentTimeMillis() - 7L * 86_400_000
    val lastWeek = data.history.filter { it.marked() && it.startMs >= weekAgo }
    if (lastWeek.size >= 5 && lastWeek.all { it.state == "present" }) {
        val wk = Calendar.getInstance().run { "${get(Calendar.YEAR)}w${get(Calendar.WEEK_OF_YEAR)}" }
        out += Badge("perfect-week-$wk", "7d", "Perfect", "Perfect week", "You attended all ${lastWeek.size} classes in the last 7 days.")
    }
    val streak = presentStreak(data.history)
    for (n in listOf(10, 25, 50)) if (streak >= n) {
        out += Badge("streak-$n", "$n", "Streak", "$n-class streak", "$streak classes in a row without missing one.")
    }
    val cgpa = data.results?.semesters?.lastOrNull()?.cgpa ?: 0.0
    if (cgpa >= 9) out += Badge("cgpa-9", "%.1f".format(cgpa), "Scholar", "CGPA 9+", "Your CGPA is %.2f.".format(cgpa))
    else if (cgpa >= 8) out += Badge("cgpa-8", "%.1f".format(cgpa), "Achiever", "CGPA 8+", "Your CGPA is %.2f.".format(cgpa))
    return out
}

val ALL_BADGES = listOf(
    Badge("l1", "75%", "Steady", "All subjects on track", "Every subject at or above your target."),
    Badge("l2", "7d", "Perfect", "Perfect week", "Attend every class for 7 days."),
    Badge("l3", "10", "Streak", "10-class streak", "10 classes in a row."),
    Badge("l4", "25", "Streak", "25-class streak", "25 classes in a row."),
    Badge("l5", "50", "Streak", "50-class streak", "50 classes in a row."),
    Badge("l6", "8.0", "Achiever", "CGPA 8+", "Reach a CGPA of 8."),
    Badge("l7", "9.0", "Scholar", "CGPA 9+", "Reach a CGPA of 9."),
)
