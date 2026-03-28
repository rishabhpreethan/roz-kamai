package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.CustomerProfileDao
import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.dao.HourlyStatsDao
import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.local.entity.HourlyStatsEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import timber.log.Timber
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triggered after each successful TransactionProjector write.
 * Re-reads all transactions for the date, recomputes all aggregates, then:
 *   - upserts DailySummaryEntity (read model for dashboard)
 *   - upserts HourlyStatsEntity (read model for hourly breakdown)
 *   - appends DailySummaryComputed event to the event store
 *
 * Covered tasks:
 *   P2-001  Aggregation automation (triggered from TransactionProjector)
 *   P2-002  Daily income total
 *   P2-003  Hourly distribution
 *   P2-008  Transaction count
 *   P2-013  Peak hour identification
 *   P2-015  First and last sale time
 *   P2-018  Payment method split (UPI vs bank)
 *   Plus delegation to InsightCalculator for P2-005, P2-006, P2-019
 */
@Singleton
class AggregationEngine @Inject constructor(
    private val transactionDao: TransactionDao,
    private val dailySummaryDao: DailySummaryDao,
    private val hourlyStatsDao: HourlyStatsDao,
    private val customerProfileDao: CustomerProfileDao,
    private val insightCalculator: InsightCalculator,
    private val eventRepository: EventRepository,
) {
    // UPI-based payment sources for split computation (P2-018)
    private val upiSources = setOf("GPAY", "PHONEPE", "PAYTM", "UPI")

    /**
     * Main entry point. Called after TransactionProjector writes to the transactions table.
     * @param transaction the newly projected transaction (used for hourly stats + run rate time)
     * @param date        the date bucket ("YYYY-MM-DD") for all queries
     */
    suspend fun aggregate(transaction: ParsedTransaction, date: String) {
        val allTodayTxns = transactionDao.getTransactionsByDate(date)
        val credits = allTodayTxns.filter { it.type == "CREDIT" }

        // P2-002: Daily totals
        val totalIncome = credits.sumOf { it.amount }
        val count = credits.size
        val avg = if (count > 0) totalIncome / count else 0.0

        // P2-015: First and last sale time
        val firstTxnTime = credits.minOfOrNull { it.timestamp }
        val lastTxnTime = credits.maxOfOrNull { it.timestamp }

        // P2-018: Payment method split
        val upiAmount = credits.filter { it.source in upiSources }.sumOf { it.amount }
        val bankAmount = credits.filter { it.source !in upiSources }.sumOf { it.amount }

        // P2-003: Update hourly stats for the incoming transaction's hour (CREDIT only)
        if (transaction.type == TransactionType.CREDIT) {
            updateHourlyStats(date, transaction.timestamp, transaction.amount)
        }

        // P2-013: Peak hour from hourly stats
        val peakHour = hourlyStatsDao.getPeakHourForDate(date)?.hourBlock

        // P2-005, P2-006, P2-019: Delegate to InsightCalculator
        val expectedIncome = insightCalculator.computeExpectedIncome(date)
        val runRate = insightCalculator.computeRunRate(totalIncome, transaction.timestamp)
        val consistencyScore = insightCalculator.computeConsistencyScore(date)

        // P2-010, P2-011, P2-012: Customer counts — profiles already updated by CustomerIdentificationService
        val newCustomers = customerProfileDao.countNewCustomersForDate(date)
        val returningCustomers = customerProfileDao.countReturningCustomersForDate(date)

        val summary = DailySummaryEntity(
            date = date,
            totalIncome = totalIncome,
            transactionCount = count,
            avgTransactionValue = avg,
            peakHour = peakHour,
            firstTxnTime = firstTxnTime,
            lastTxnTime = lastTxnTime,
            expectedIncome = expectedIncome,
            runRateProjection = runRate,
            consistencyScore = consistencyScore,
            upiAmount = upiAmount,
            bankAmount = bankAmount,
            newCustomers = newCustomers,
            returningCustomers = returningCustomers,
        )
        dailySummaryDao.upsert(summary)

        appendDailySummaryComputedEvent(summary)
        Timber.d("AggregationEngine: date=$date totalIncome=$totalIncome count=$count peakHour=$peakHour")
    }

    // ─── P2-003: Hourly stats ─────────────────────────────────────────────────

    private suspend fun updateHourlyStats(date: String, timestamp: Long, amount: Double) {
        val hour = Calendar.getInstance().also { it.timeInMillis = timestamp }
            .get(Calendar.HOUR_OF_DAY)
        val existing = hourlyStatsDao.getByDateAndHour(date, hour)
        val updated = HourlyStatsEntity(
            date = date,
            hourBlock = hour,
            txnCount = (existing?.txnCount ?: 0) + 1,
            totalAmount = (existing?.totalAmount ?: 0.0) + amount,
        )
        hourlyStatsDao.upsert(updated)
    }

    // ─── Event production ─────────────────────────────────────────────────────

    private suspend fun appendDailySummaryComputedEvent(summary: DailySummaryEntity) {
        val payload = buildString {
            append("""{"date":"${summary.date}"""")
            append(""","total_income":${summary.totalIncome}""")
            append(""","transaction_count":${summary.transactionCount}""")
            append(""","avg_transaction_value":${summary.avgTransactionValue}""")
            summary.peakHour?.let { append(""","peak_hour":$it""") }
            summary.firstTxnTime?.let { append(""","first_txn_time":$it""") }
            summary.lastTxnTime?.let { append(""","last_txn_time":$it""") }
            summary.expectedIncome?.let { append(""","expected_income":$it""") }
            summary.runRateProjection?.let { append(""","run_rate_projection":$it""") }
            summary.consistencyScore?.let { append(""","consistency_score":$it""") }
            append(""","upi_amount":${summary.upiAmount}""")
            append(""","bank_amount":${summary.bankAmount}""")
            append("}")
        }
        eventRepository.appendEvent(
            EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = "DailySummaryComputed",
                timestamp = System.currentTimeMillis(),
                payload = payload,
                version = 1,
            ),
        )
    }
}
