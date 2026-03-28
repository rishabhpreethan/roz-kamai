package com.viis.rozkamai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable event log — the source of truth.
 * NEVER UPDATE OR DELETE rows from this table.
 * All read models are derived from this event stream.
 */
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["event_type"]),
        Index(value = ["timestamp"])
    ]
)
data class EventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "payload")
    val payload: String, // JSON-serialized event data

    @ColumnInfo(name = "version")
    val version: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
