package com.viis.rozkamai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily summary read model — projection derived from DailySummaryComputed events.
 * One row per day.
 */
@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "total_income")
    val totalIncome: Double = 0.0,

    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int = 0,

    @ColumnInfo(name = "avg_transaction_value")
    val avgTransactionValue: Double = 0.0,

    @ColumnInfo(name = "peak_hour")
    val peakHour: Int? = null,

    @ColumnInfo(name = "first_txn_time")
    val firstTxnTime: Long? = null,

    @ColumnInfo(name = "last_txn_time")
    val lastTxnTime: Long? = null,

    @ColumnInfo(name = "expected_income")
    val expectedIncome: Double? = null,

    @ColumnInfo(name = "run_rate_projection")
    val runRateProjection: Double? = null,

    @ColumnInfo(name = "consistency_score")
    val consistencyScore: Double? = null,

    @ColumnInfo(name = "new_customers")
    val newCustomers: Int = 0,

    @ColumnInfo(name = "returning_customers")
    val returningCustomers: Int = 0,

    @ColumnInfo(name = "upi_amount")
    val upiAmount: Double = 0.0,

    @ColumnInfo(name = "bank_amount")
    val bankAmount: Double = 0.0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
