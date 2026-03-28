package com.viis.rozkamai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Customer profile read model — derived from CustomerIdentified events.
 * customer_id is a hash of upi_handle or normalized name — never raw PII as the key.
 */
@Entity(
    tableName = "customer_profiles",
    indices = [Index(value = ["transaction_count"])]
)
data class CustomerProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "customer_id")
    val customerId: String, // hash(upiHandle ?: normalizedName)

    @ColumnInfo(name = "display_name")
    val displayName: String?,

    @ColumnInfo(name = "upi_handle_hash")
    val upiHandleHash: String?, // hashed for lookup, never raw

    @ColumnInfo(name = "first_seen")
    val firstSeen: Long,

    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,

    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int = 0,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
