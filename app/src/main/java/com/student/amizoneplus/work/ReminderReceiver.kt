package com.student.amizoneplus.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Shows a class / exam reminder scheduled by [Notifier]. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val id = intent.getIntExtra("id", title.hashCode())
        val body = intent.getStringExtra("body").orEmpty()
        val kind = intent.getStringExtra("kind").orEmpty()
        if (kind.isEmpty()) return Notifier.show(context, id, Notifier.CH_REMINDERS, title, body)
        Notifier.showCard(
            context, id, title, body,
            tag = intent.getStringExtra("tag").orEmpty(), name = intent.getStringExtra("name").orEmpty(), detail = body, kind = kind,
            mood = if (intent.getStringExtra("mood") == "sleep") NotifArt.Mood.Sleep else NotifArt.Mood.Sit,
        )
    }
}
