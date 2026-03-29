package com.viis.rozkamai.infrastructure.worker

import com.viis.rozkamai.data.local.dao.DailySummaryDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure logic for the mid-day underperformance alert (P3-004).
 * Extracted from MidDayAlertWorker for unit testability without Android context.
 */
@Singleton
class MidDayAlertLogic @Inject constructor(
    private val dailySummaryDao: DailySummaryDao,
) {
    /**
     * Returns [MidDayAlertContent] if an alert should be sent, null otherwise.
     *
     * Alert fires when:
     *   - A daily summary exists for [date]
     *   - An expected income baseline is available
     *   - Current income is below [INCOME_THRESHOLD] (70%) of expected
     */
    suspend fun computeAlertContent(date: String): MidDayAlertContent? {
        val summary = dailySummaryDao.getSummaryForDate(date) ?: return null
        val expected = summary.expectedIncome ?: return null
        if (expected <= 0.0) return null
        if (summary.totalIncome >= expected * INCOME_THRESHOLD) return null
        return MidDayAlertContent(
            currentIncome = summary.totalIncome,
            expectedIncome = expected,
        )
    }

    companion object {
        /** Alert fires when current income is below this fraction of expected. */
        const val INCOME_THRESHOLD = 0.70
    }
}

/** Payload for [com.viis.rozkamai.infrastructure.notification.NotificationEngine.sendMidDayAlert]. */
data class MidDayAlertContent(
    val currentIncome: Double,
    val expectedIncome: Double,
)
