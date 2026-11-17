package com.student.amizoneplus.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.student.amizoneplus.data.Repository
import java.util.concurrent.TimeUnit

/**
 * Keeps the Amizone session warm. Amizone signs you out after a stretch of no activity,
 * and signing in again sometimes needs its human check. A tiny request every ~15 minutes
 * (Android may stretch this while the phone sleeps) keeps the session from going idle,
 * so opening the app hours later usually needs no sign-in at all.
 */
class KeepAliveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = Repository.get(applicationContext)
        if (repo.storage.loadCredentials() == null) return Result.success()
        runCatching { repo.client.keepAlive() }
        return Result.success()
    }

    companion object {
        private const val NAME = "amizone-keepalive"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
