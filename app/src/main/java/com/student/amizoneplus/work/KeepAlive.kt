package com.student.amizoneplus.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.student.amizoneplus.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the Amizone session alive.
 *
 * Amizone now demands a CAPTCHA ("human check") for every new sign-in, so the app can't
 * quietly sign in again once the session has lapsed. Instead it pings Amizone with a tiny
 * signed-in request every ~10 minutes, which keeps the session from timing out.
 *
 * WorkManager is far too lazy for this (Android ran it 3 times in 13 hours); an
 * allow-while-idle alarm fires even in Doze and briefly allows network access. Each ping
 * schedules the next one, so the chain survives on its own; it is restarted on app start,
 * after a reboot and after an update.
 */
object KeepAlive {
    private const val INTERVAL_MS = 10 * 60_000L
    private const val REQ = 7101

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQ, Intent(context, KeepAliveReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun schedule(context: Context, delayMs: Long = INTERVAL_MS) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + delayMs
        val pi = pending(context)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context))
    }
}

class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val repo = Repository.get(app)
        if (repo.storage.loadCredentials() == null) return // signed out: let the chain end
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alive = withTimeoutOrNull(20_000) { runCatching { repo.client.keepAlive() }.getOrDefault(false) }
                Log.i("AmzKeepAlive", "ping alive=$alive")
            } finally {
                KeepAlive.schedule(app)
                done.finish()
            }
        }
    }
}
