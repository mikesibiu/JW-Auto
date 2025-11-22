package org.jw.library.auto.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Scheduler for background content sync
 */
object ContentSyncScheduler {

    /**
     * Schedule periodic content sync
     * Runs once every 24 hours when on WiFi
     */
    fun schedulePeriodicSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<ContentSyncWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ContentSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Kick off a one-time sync so fresh content is available immediately after install.
     */
    fun scheduleImmediateSync(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ContentSyncWorker>()
            .setConstraints(defaultConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${ContentSyncWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun defaultConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /**
     * Cancel scheduled sync
     */
    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ContentSyncWorker.WORK_NAME)
    }
}
