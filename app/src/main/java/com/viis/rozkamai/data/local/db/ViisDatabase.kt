package com.viis.rozkamai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.viis.rozkamai.data.local.dao.CustomerProfileDao
import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.dao.EventDao
import com.viis.rozkamai.data.local.dao.HourlyStatsDao
import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.CustomerProfileEntity
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.local.entity.HourlyStatsEntity
import com.viis.rozkamai.data.local.entity.TransactionEntity

@Database(
    entities = [
        EventEntity::class,
        TransactionEntity::class,
        DailySummaryEntity::class,
        HourlyStatsEntity::class,
        CustomerProfileEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class ViisDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun transactionDao(): TransactionDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun hourlyStatsDao(): HourlyStatsDao
    abstract fun customerProfileDao(): CustomerProfileDao

    companion object {
        const val DATABASE_NAME = "viis_db"
    }
}
