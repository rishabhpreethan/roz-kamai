package com.viis.rozkamai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Customer profile read model — derived from CustomerIdentified events.
 * customer_id is the SHA-256 hash of the UPI handle — never raw PII as the key.
 *
 * Privacy rules:
 *   - customerId / upiHandleHash: SHA-256 hash only — never the raw UPI handle
 *   - displayName: never populated — field kept for schema compatibility only
 *   - firstSeenDate / lastSeenDate: stored as "YYYY-MM-DD" in device local timezone
 *     so queries don't need to convert epoch millis (which would be UTC-biased)
 */
@Entity(
    tableName = "customer_profiles",
    indices = [
        Index(value = ["transaction_count"]),
        Index(value = ["first_seen_date"]),
        Index(value = ["last_seen_date"]),
    ]
)
data class CustomerProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "customer_id")
    val customerId: String, // SHA-256 of UPI handle — never raw

    @ColumnInfo(name = "display_name")
    val displayName: String?,  // always null — field reserved, never populated

    @ColumnInfo(name = "upi_handle_hash")
    val upiHandleHash: String?, // same as customerId — retained for explicit lookup

    @ColumnInfo(name = "first_seen")
    val firstSeen: Long,

    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,

    @ColumnInfo(name = "first_seen_date")
    val firstSeenDate: String,  // "YYYY-MM-DD" — device local timezone

    @ColumnInfo(name = "last_seen_date")
    val lastSeenDate: String,   // "YYYY-MM-DD" — device local timezone

    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int = 0,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
