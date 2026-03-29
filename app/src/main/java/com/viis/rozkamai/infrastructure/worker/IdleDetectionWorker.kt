package com.viis.rozkamai.infrastructure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.infrastructure.notification.NotificationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Periodic worker (every 30 minutes) that detects idle gaps during business hours.
 * P2-014: Idle time detection.
 *
 * If no CREDIT transaction has been recorded for [IDLE_THRESHOLD_MINUTES] during
 * business hours (09:00–21:00), appends an IdleDetected event to the event store.
 *
 * Note: this worker fires an event on every run where the threshold is exceeded —
 * the notification layer (Phase 3) is responsible for deciding whether to alert the user.
 */
@HiltWorker
class IdleDetectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val eventRepository: EventRepository,
    private val notificationEngine: NotificationEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().also { it.timeInMillis = now }
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)

        // Only check during business hours
        if (hourOfDay < BUSINESS_START_HOUR || hourOfDay >= BUSINESS_END_HOUR) {
            return Result.success()
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val lastTxnTime = transactionDao.getLastTransactionTime(today)

        val gapMinutes: Long = if (lastTxnTime != null) {
            (now - lastTxnTime) / 60_000L
        } else {
            // No transactions today — measure gap from start of business hours
            val businessStartMs = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, BUSINESS_START_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            (now - businessStartMs) / 60_000L
        }

        if (gapMinutes >= IDLE_THRESHOLD_MINUTES) {
            appendIdleDetectedEvent(gapMinutes, lastTxnTime ?: 0L, now)
            notificationEngine.sendInactivityAlert(gapMinutes)
            Timber.d("IdleDetectionWorker: gap=${gapMinutes}min >= threshold=${IDLE_THRESHOLD_MINUTES}min")
        }

        return Result.success()
    }

    private suspend fun appendIdleDetectedEvent(gapMinutes: Long, lastTxnTime: Long, detectedAt: Long) {
        eventRepository.appendEvent(
            EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = "IdleDetected",
                timestamp = detectedAt,
                payload = """{"gap_minutes":$gapMinutes,"last_txn_time":$lastTxnTime,"threshold":$IDLE_THRESHOLD_MINUTES}""",
                version = 1,
            ),
        )
    }

    companion object {
        const val WORK_NAME = "viis_idle_detection"
        const val IDLE_THRESHOLD_MINUTES = 120L  // 2 hours
        const val BUSINESS_START_HOUR = 9
        const val BUSINESS_END_HOUR = 21
    }
}
