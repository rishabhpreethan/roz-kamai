package com.viis.rozkamai.infrastructure.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules all periodic background workers.
 * Called from RozKamaiApp.onCreate() and BootReceiver (P3-009).
 * WorkManager deduplicates via KEEP policy, so re-calling is safe.
 */
@Singleton
class WorkScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun schedulePeriodicWorkers() {
        scheduleIdleDetection()
        scheduleEodSummary()
        scheduleMidDayAlert()
    }

    // ─── Idle detection (P2-014) ──────────────────────────────────────────────

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

    // ─── EOD summary (P3-003) ─────────────────────────────────────────────────

    private fun scheduleEodSummary() {
        val initialDelay = calculateDelayToHour(EOD_TARGET_HOUR)
        val request = PeriodicWorkRequestBuilder<EodSummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            EodSummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Timber.d("WorkScheduler: EOD summary scheduled (delay=${initialDelay / 60_000}min)")
    }

    // ─── Mid-day alert (P3-004) ───────────────────────────────────────────────

    private fun scheduleMidDayAlert() {
        val initialDelay = calculateDelayToHour(MIDDAY_TARGET_HOUR)
        val request = PeriodicWorkRequestBuilder<MidDayAlertWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            MidDayAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Timber.d("WorkScheduler: mid-day alert scheduled (delay=${initialDelay / 60_000}min)")
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Calculates milliseconds until the next occurrence of [targetHour]:00 local time.
     * If that time has already passed today, returns delay to tomorrow's occurrence.
     */
    private fun calculateDelayToHour(targetHour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        private const val EOD_TARGET_HOUR = 21    // 9 PM
        private const val MIDDAY_TARGET_HOUR = 14  // 2 PM
    }
}
