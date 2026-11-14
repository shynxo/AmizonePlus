package com.student.amizoneplus.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Shows a class / exam reminder scheduled by [Notifier]. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        Notifier.show(context, intent.getIntExtra("id", title.hashCode()), Notifier.CH_REMINDERS, title, intent.getStringExtra("body").orEmpty())
    }
}
