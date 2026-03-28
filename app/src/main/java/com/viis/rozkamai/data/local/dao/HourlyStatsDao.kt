package com.viis.rozkamai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.viis.rozkamai.data.local.entity.HourlyStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HourlyStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: HourlyStatsEntity)

    @Query("SELECT * FROM hourly_stats WHERE date = :date ORDER BY hour_block ASC")
    fun observeHourlyStats(date: String): Flow<List<HourlyStatsEntity>>

    @Query("SELECT * FROM hourly_stats WHERE date = :date ORDER BY hour_block ASC")
    suspend fun getHourlyStatsForDate(date: String): List<HourlyStatsEntity>

    @Query("SELECT * FROM hourly_stats WHERE date = :date AND hour_block = :hour LIMIT 1")
    suspend fun getByDateAndHour(date: String, hour: Int): HourlyStatsEntity?

    @Query("SELECT * FROM hourly_stats WHERE date = :date ORDER BY total_amount DESC LIMIT 1")
    suspend fun getPeakHourForDate(date: String): HourlyStatsEntity?
}
