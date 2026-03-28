package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.dao.HourlyStatsDao
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stateless insight computations that read from historical read models.
 * Does not write to any table or the event store — AggregationEngine owns all writes.
 *
 * Covered tasks:
 *   P2-004  Day-over-day income comparison
 *   P2-005  Expected earnings baseline (14-day rolling same-weekday average)
 *   P2-006  Live run rate projection to end of business day
 *   P2-007  Slow hour detection (hours below 50% of today's hourly average)
 *   P2-009  Average sale value (computed per-call from provided inputs)
 *   P2-013  Peak hour identification (via HourlyStatsDao)
 *   P2-015  First and last sale time (provided from AggregationEngine)
 *   P2-018  Payment method split (UPI vs bank, computed per-call from provided inputs)
 *   P2-019  Consistency score (days with income > 0 / last 7 days)
 */
@Singleton
class InsightCalculator @Inject constructor(
    private val dailySummaryDao: DailySummaryDao,
    private val hourlyStatsDao: HourlyStatsDao,
) {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ─── P2-005: Expected earnings baseline ───────────────────────────────────

    /**
     * 14-day rolling average for the same weekday, excluding today.
     * Returns null if fewer than 3 historical data points exist (not enough to be reliable).
     */
    suspend fun computeExpectedIncome(date: String): Double? {
        val weekday = getWeekday(date) ?: return null
        val history = dailySummaryDao.getSummariesForWeekday(weekday, limit = 15)
            .filter { it.date != date }
            .take(14)
        if (history.size < 3) return null
        return history.map { it.totalIncome }.average()
    }

    // ─── P2-006: Run rate projection ──────────────────────────────────────────

    /**
     * Projects current income to end of business day (22:00).
     * Returns null if before 09:30 (too early for a meaningful projection)
     * or if no income has been recorded yet.
     */
    fun computeRunRate(currentIncome: Double, currentTimestamp: Long): Double? {
        if (currentIncome <= 0) return null
        val cal = Calendar.getInstance().also { it.timeInMillis = currentTimestamp }
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val businessStartMin = 9 * 60    // 09:00
        val businessEndMin = 22 * 60     // 22:00
        val businessDuration = businessEndMin - businessStartMin

        val elapsed = minuteOfDay - businessStartMin
        if (elapsed < 30) return null    // avoid wild projections in first 30 min
        val rate = currentIncome / elapsed.toDouble()
        return rate * businessDuration
    }

    // ─── P2-007: Slow hour detection ──────────────────────────────────────────

    /**
     * Returns hour blocks where income is below 50% of today's average hourly income.
     * Returns empty list if fewer than 3 active hours (insufficient data).
     */
    suspend fun computeSlowHours(date: String): List<Int> {
        val stats = hourlyStatsDao.getHourlyStatsForDate(date)
        val active = stats.filter { it.txnCount > 0 }
        if (active.size < 3) return emptyList()
        val avgIncome = active.map { it.totalAmount }.average()
        return active.filter { it.totalAmount < avgIncome * 0.5 }.map { it.hourBlock }
    }

    // ─── P2-004: Day-over-day comparison ──────────────────────────────────────

    /**
     * Returns yesterday's total income for comparison, or null if no record exists.
     */
    suspend fun computeDayOverDayIncome(date: String): Double? {
        val yesterday = getPreviousDate(date) ?: return null
        return dailySummaryDao.getSummaryForDate(yesterday)?.totalIncome
    }

    // ─── P2-019: Consistency score ────────────────────────────────────────────

    /**
     * Fraction of the last 7 days (excluding today) where income > 0.
     * Range: 0.0–1.0. Returns null if fewer than 7 days of history exist.
     */
    suspend fun computeConsistencyScore(date: String): Double? {
        val recent = dailySummaryDao.getRecentSummaries(limit = 8)
            .filter { it.date != date }
            .take(7)
        if (recent.size < 7) return null
        return recent.count { it.totalIncome > 0 }.toDouble() / 7.0
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns SQLite strftime-compatible weekday string ("0"=Sunday … "6"=Saturday).
     */
    fun getWeekday(date: String): String? = runCatching {
        val cal = Calendar.getInstance()
        cal.time = dateFmt.parse(date)!!
        // Calendar.DAY_OF_WEEK: 1=Sunday…7=Saturday → subtract 1 for SQLite '%w' format
        (cal.get(Calendar.DAY_OF_WEEK) - 1).toString()
    }.onFailure { Timber.e(it, "InsightCalculator: failed to parse date $date") }
        .getOrNull()

    private fun getPreviousDate(date: String): String? = runCatching {
        val cal = Calendar.getInstance()
        cal.time = dateFmt.parse(date)!!
        cal.add(Calendar.DAY_OF_MONTH, -1)
        dateFmt.format(cal.time)
    }.getOrNull()
}
