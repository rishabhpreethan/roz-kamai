package com.viis.rozkamai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    fun observeSummaryForDate(date: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    suspend fun getSummaryForDate(date: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSummaries(limit: Int): List<DailySummaryEntity>

    @Query("""
        SELECT * FROM daily_summaries
        WHERE strftime('%w', date) = :weekday
        ORDER BY date DESC
        LIMIT :limit
    """)
    suspend fun getSummariesForWeekday(weekday: String, limit: Int = 14): List<DailySummaryEntity>
}
