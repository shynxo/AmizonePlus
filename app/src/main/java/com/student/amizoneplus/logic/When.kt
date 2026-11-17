package com.student.amizoneplus.logic

import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/** How long an exam is treated as "happening now" after it starts. */
const val EXAM_LENGTH_MS = 3 * 3_600_000L

/** Calendar days from [now] to [t] (0 = same day, 1 = tomorrow), not 24-hour blocks. */
fun daysUntil(t: Long, now: Long = System.currentTimeMillis()): Int {
    val z = ZoneId.systemDefault()
    return ChronoUnit.DAYS.between(Instant.ofEpochMilli(now).atZone(z).toLocalDate(), Instant.ofEpochMilli(t).atZone(z).toLocalDate()).toInt()
}

/** "happening now" · "today at 10:00 am" · "tomorrow at 2:00 pm" · "in 4 days" · "done". */
fun examWhen(t: Long, now: Long = System.currentTimeMillis()): String {
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t))
    val d = daysUntil(t, now)
    return when {
        now >= t && now < t + EXAM_LENGTH_MS -> "happening now"
        now >= t -> "done"
        d == 0 -> "today at $time"
        d == 1 -> "tomorrow at $time"
        else -> "in $d days"
    }
}
