package com.student.amizoneplus.data

import android.content.Context
import com.student.amizoneplus.work.Notifier

/**
 * Single place that knows how to refresh: fetch from Amizone, keep previous
 * values for sections that failed, cache, then update alerts and reminders.
 * Used by both the UI and the background worker.
 */
class Repository private constructor(context: Context) {
    val storage = Storage(context)
    val client = AmizoneClient { storage.loadCredentials() }
    val lms = Lms(client)
    private val app = context.applicationContext

    suspend fun refreshAll(): AmizoneData {
        val fresh = client.fetchAll()
        val old = storage.loadCache()
        val merged = fresh.copy(
            profile = fresh.profile ?: old?.profile,
            exams = fresh.exams ?: old?.exams,
            courses = fresh.courses.ifEmpty { old?.courses.orEmpty() },
            attendance = fresh.attendance.ifEmpty { old?.attendance.orEmpty() },
            schedule = if ("Timetable" in fresh.errors) old?.schedule.orEmpty() else fresh.schedule,
            history = fresh.history.ifEmpty { old?.history.orEmpty() },
            results = fresh.results ?: old?.results,
            notices = fresh.notices.ifEmpty { old?.notices.orEmpty() },
            credits = fresh.credits.ifEmpty { old?.credits.orEmpty() },
            hours = fresh.hours.ifEmpty { old?.hours.orEmpty() },
        )
        if (fresh.emailPassword.isNotEmpty()) storage.saveSecret("email_pw", fresh.emailPassword)
        storage.saveCache(merged)
        Notifier.process(app, merged, storage.loadSettings(), storage)
        return merged
    }

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository =
            instance ?: synchronized(this) { instance ?: Repository(context).also { instance = it } }
    }
}
