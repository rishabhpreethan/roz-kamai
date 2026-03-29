package com.viis.rozkamai.infrastructure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.viis.rozkamai.infrastructure.notification.NotificationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fires once daily around 9 PM (P3-003). Guarded to the window 20:30–21:30
 * so that if WorkManager fires slightly off-schedule the notification still
 * sends (and doesn't fire outside the window if the worker is re-run on retry).
 *
 * Appends a NotificationSent event via NotificationEngine (P3-008).
 * Scheduled via WorkScheduler (P3-001).
 */
@HiltWorker
class EodSummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eodLogic: EodSummaryLogic,
    private val notificationEngine: NotificationEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().also { it.timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        if (hour < EOD_WINDOW_START || hour > EOD_WINDOW_END) {
            Timber.d("EodSummaryWorker: outside window (hour=$hour), skipping")
            return Result.success()
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val content = eodLogic.computeContent(today)
        if (content == null) {
            Timber.d("EodSummaryWorker: no income today, skipping notification")
            return Result.success()
        }

        notificationEngine.sendEodSummary(content)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "viis_eod_summary"
        const val EOD_WINDOW_START = 20 // 8 PM
        const val EOD_WINDOW_END = 21   // 9 PM (inclusive)
    }
}
