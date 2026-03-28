package com.viis.rozkamai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.viis.rozkamai.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    /**
     * Append event to the immutable log. NEVER update or delete.
     * OnConflictStrategy.ABORT ensures duplicate event_ids fail loudly.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun appendEvent(event: EventEntity)

    @Query("SELECT * FROM events WHERE event_type = :eventType ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsByType(eventType: String, limit: Int = 100): List<EventEntity>

    @Query("SELECT * FROM events WHERE timestamp >= :fromTimestamp ORDER BY timestamp ASC")
    suspend fun getEventsSince(fromTimestamp: Long): List<EventEntity>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun observeAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun getEventCount(): Long

    /**
     * Used by DeduplicationChecker — finds TransactionDetected events in a time window
     * whose payload contains the given amount string. Payload filtering is done in-memory
     * after this query to avoid LIKE on JSON (no index benefit anyway at this scale).
     */
    @Query(
        """SELECT * FROM events
           WHERE event_type = 'TransactionDetected'
           AND timestamp BETWEEN :fromTimestamp AND :toTimestamp"""
    )
    suspend fun getTransactionDetectedInWindow(fromTimestamp: Long, toTimestamp: Long): List<EventEntity>
}
