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
 * Fires once daily around 2 PM (P3-004). Sends an alert if current income is
 * below 70% of the expected baseline — only when that baseline is available
 * (requires at least 3 same-weekday data points from InsightCalculator P2-005).
 *
 * Window-guarded to 13:30–14:30 for the same reason as EodSummaryWorker.
 * Appends a NotificationSent event via NotificationEngine (P3-008).
 */
@HiltWorker
class MidDayAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val midDayLogic: MidDayAlertLogic,
    private val notificationEngine: NotificationEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().also { it.timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        if (hour < MIDDAY_WINDOW_START || hour > MIDDAY_WINDOW_END) {
            Timber.d("MidDayAlertWorker: outside window (hour=$hour), skipping")
            return Result.success()
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val content = midDayLogic.computeAlertContent(today)
        if (content == null) {
            Timber.d("MidDayAlertWorker: no alert needed today")
            return Result.success()
        }

        notificationEngine.sendMidDayAlert(content)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "viis_midday_alert"
        const val MIDDAY_WINDOW_START = 13 // 1 PM
        const val MIDDAY_WINDOW_END = 14   // 2 PM (inclusive)
    }
}
