package com.viis.rozkamai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Hourly stats read model — projection of transactions by hour block.
 * Composite primary key: date + hour_block.
 */
@Entity(
    tableName = "hourly_stats",
    primaryKeys = ["date", "hour_block"]
)
data class HourlyStatsEntity(
    @ColumnInfo(name = "date")
    val date: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "hour_block")
    val hourBlock: Int, // 0–23

    @ColumnInfo(name = "txn_count")
    val txnCount: Int = 0,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
