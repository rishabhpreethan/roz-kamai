package com.viis.rozkamai.infrastructure.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules all periodic background workers.
 * Called once from RozKamaiApp.onCreate() — WorkManager deduplicates via KEEP policy,
 * so re-calling on subsequent launches is safe.
 */
@Singleton
class WorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun schedulePeriodicWorkers() {
        scheduleIdleDetection()
    }

    private fun scheduleIdleDetection() {
        val request = PeriodicWorkRequestBuilder<IdleDetectionWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            IdleDetectionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Timber.d("WorkScheduler: idle detection scheduled (every 30 min)")
    }
}
