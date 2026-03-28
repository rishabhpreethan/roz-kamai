package com.viis.rozkamai.data.repository

import com.viis.rozkamai.data.local.entity.EventEntity

/**
 * Contract for the append-only event store.
 * All writes go through this interface — no direct DAO access from workers or receivers.
 */
interface EventRepository {
    suspend fun appendEvent(event: EventEntity)
    suspend fun getTransactionDetectedInWindow(fromTimestamp: Long, toTimestamp: Long): List<EventEntity>
}
