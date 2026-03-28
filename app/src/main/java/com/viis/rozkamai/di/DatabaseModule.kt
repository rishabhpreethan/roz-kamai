package com.viis.rozkamai.di

import android.content.Context
import androidx.room.Room
import com.viis.rozkamai.data.local.db.ViisDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ViisDatabase =
        Room.databaseBuilder(context, ViisDatabase::class.java, ViisDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideEventDao(db: ViisDatabase) = db.eventDao()

    @Provides
    fun provideTransactionDao(db: ViisDatabase) = db.transactionDao()

    @Provides
    fun provideDailySummaryDao(db: ViisDatabase) = db.dailySummaryDao()

    @Provides
    fun provideHourlyStatsDao(db: ViisDatabase) = db.hourlyStatsDao()

    @Provides
    fun provideCustomerProfileDao(db: ViisDatabase) = db.customerProfileDao()
}
