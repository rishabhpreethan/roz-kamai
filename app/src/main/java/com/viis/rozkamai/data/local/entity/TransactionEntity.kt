package com.viis.rozkamai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transaction read model — projection derived from TransactionDetected events.
 * Can be rebuilt from the event store if corrupted.
 *
 * Privacy rules:
 *   - upiHandleHash: SHA-256 of the raw UPI handle — never the raw handle
 *   - rawSenderMasked: first 4 chars + "***" — never the full sender ID
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["date_bucket"]),
        Index(value = ["event_id"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "event_id")
    val eventId: String, // logical reference to events.event_id

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "type")
    val type: String, // TransactionType.name — CREDIT or DEBIT

    @ColumnInfo(name = "source")
    val source: String, // PaymentSource.name

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "date_bucket")
    val dateBucket: String, // "YYYY-MM-DD" for fast day queries

    @ColumnInfo(name = "raw_sender_masked")
    val rawSenderMasked: String, // e.g. "GPAY***"

    @ColumnInfo(name = "upi_handle_hash")
    val upiHandleHash: String?, // SHA-256 of UPI handle, null if not available

    @ColumnInfo(name = "reference_id")
    val referenceId: String?,

    @ColumnInfo(name = "status")
    val status: String = "SUCCESS",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
