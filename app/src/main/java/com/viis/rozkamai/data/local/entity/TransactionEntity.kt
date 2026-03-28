package com.viis.rozkamai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transaction read model — projection derived from TransactionDetected events.
 * Can be rebuilt from the event store if corrupted.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["date_bucket"]),
        Index(value = ["event_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "date_bucket")
    val dateBucket: String, // "YYYY-MM-DD" for fast day queries

    @ColumnInfo(name = "sender_name")
    val senderName: String?,

    @ColumnInfo(name = "upi_handle")
    val upiHandle: String?,

    @ColumnInfo(name = "source")
    val source: String, // PaymentSource.name

    @ColumnInfo(name = "reference_id")
    val referenceId: String?,

    @ColumnInfo(name = "status")
    val status: String, // TransactionStatus.name

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
