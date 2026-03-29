package com.viis.rozkamai.infrastructure.worker

import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.domain.usecase.InsightCalculator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure logic for the EOD summary notification (P3-003).
 * Extracted from EodSummaryWorker for unit testability without Android context.
 */
@Singleton
class EodSummaryLogic @Inject constructor(
    private val dailySummaryDao: DailySummaryDao,
    private val insightCalculator: InsightCalculator,
) {
    /**
     * Returns [EodContent] if there is income data for [date], null otherwise.
     * Null means the worker should skip the notification.
     */
    suspend fun computeContent(date: String): EodContent? {
        val summary = dailySummaryDao.getSummaryForDate(date) ?: return null
        if (summary.transactionCount == 0) return null
        return EodContent(
            totalIncome = summary.totalIncome,
            transactionCount = summary.transactionCount,
            dayOverDayIncome = insightCalculator.computeDayOverDayIncome(date),
        )
    }
}

/** Payload for [com.viis.rozkamai.infrastructure.notification.NotificationEngine.sendEodSummary]. */
data class EodContent(
    val totalIncome: Double,
    val transactionCount: Int,
    val dayOverDayIncome: Double?,
)
