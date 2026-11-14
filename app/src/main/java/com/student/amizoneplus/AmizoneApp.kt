package com.student.amizoneplus

import android.app.Application
import com.student.amizoneplus.work.Notifier

class AmizoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.student.amizoneplus.data.Http.init(this)
        Notifier.createChannels(this)
    }
}
