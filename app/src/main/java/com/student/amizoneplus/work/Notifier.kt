package com.student.amizoneplus.work

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.student.amizoneplus.MainActivity
import com.student.amizoneplus.R
import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.data.Storage
import com.student.amizoneplus.logic.percent
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Low-attendance alerts (immediate) and class / exam reminders (AlarmManager). */
object Notifier {
    const val CH_REMINDERS = "class-reminders"
    const val CH_ALERTS = "attendance-alerts"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REMINDERS, "Class & exam reminders", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_ALERTS, "Low attendance alerts", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun canPost(context: Context) =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, id: Int, channel: String, title: String, body: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun process(context: Context, data: AmizoneData, s: Settings, storage: Storage) {
        lowAttendance(context, data, s, storage)
        scheduleReminders(context, data, s, storage)
    }

    /** Alert once per course when it drops below the target; re-arm when it recovers. */
    private fun lowAttendance(context: Context, data: AmizoneData, s: Settings, storage: Storage) {
        val alerted = storage.alertedCourses.toMutableSet()
        for (a in data.attendance) {
            if (a.held == 0) continue
            val p = percent(a.attended, a.held)
            if (p < s.threshold && a.code !in alerted) {
                alerted += a.code
                if (s.lowAttendanceAlerts) {
                    show(context, a.code.hashCode(), CH_ALERTS, "Low attendance: ${a.name}",
                        "You're at %.1f%% (%d/%d), below %d%%.".format(p, a.attended, a.held, s.threshold))
                }
            } else if (p >= s.threshold) {
                alerted -= a.code
            }
        }
        storage.alertedCourses = alerted
    }

    private fun scheduleReminders(context: Context, data: AmizoneData, s: Settings, storage: Storage) {
        val am = context.getSystemService(AlarmManager::class.java)
        // Cancel what we scheduled last time.
        for (id in storage.reminderIds) am.cancel(pending(context, id.toInt(), "", ""))
        val now = System.currentTimeMillis()
        val ids = mutableSetOf<String>()
        val tf = DateFormat.getTimeInstance(DateFormat.SHORT)

        fun at(time: Long, title: String, body: String) {
            if (time <= now || ids.size >= 60) return
            val id = (title + time).hashCode()
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(context, id, title, body))
            ids += id.toString()
        }

        if (s.classReminders) for (c in data.schedule) {
            at(c.startMs - s.reminderMinutes * 60_000L, "${c.name} in ${s.reminderMinutes} min",
                listOf(tf.format(Date(c.startMs)), c.room.takeIf { it.isNotBlank() }?.let { "Room $it" }, c.faculty)
                    .filter { !it.isNullOrBlank() }.joinToString(" · "))
        }
        if (s.examReminders) for (e in data.exams?.exams.orEmpty()) {
            val t = e.timeMs ?: continue
            val eve = Calendar.getInstance().apply { timeInMillis = t; add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0) }
            val body = listOf(tf.format(Date(t)), e.mode, e.location).filter { it.isNotBlank() }.joinToString(" · ")
            at(eve.timeInMillis, "Exam tomorrow: ${e.name}", body)
            at(t - 3_600_000L, "Exam in 1 hour: ${e.name}", body)
        }
        storage.reminderIds = ids
    }

    private fun pending(context: Context, id: Int, title: String, body: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, id,
            Intent(context, ReminderReceiver::class.java).putExtra("title", title).putExtra("body", body).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
