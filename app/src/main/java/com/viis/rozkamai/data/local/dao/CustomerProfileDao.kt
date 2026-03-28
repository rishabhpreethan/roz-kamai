package com.viis.rozkamai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.viis.rozkamai.data.local.entity.CustomerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CustomerProfileEntity)

    @Query("SELECT * FROM customer_profiles ORDER BY transaction_count DESC LIMIT :limit")
    fun observeTopByFrequency(limit: Int = 10): Flow<List<CustomerProfileEntity>>

    @Query("SELECT * FROM customer_profiles ORDER BY total_amount DESC LIMIT :limit")
    fun observeTopByValue(limit: Int = 10): Flow<List<CustomerProfileEntity>>

    @Query("SELECT * FROM customer_profiles WHERE customer_id = :customerId")
    suspend fun getById(customerId: String): CustomerProfileEntity?

    /** P2-012: Customers whose first transaction was on this date (new customers). */
    @Query("SELECT COUNT(*) FROM customer_profiles WHERE first_seen_date = :date")
    suspend fun countNewCustomersForDate(date: String): Int

    /** P2-011: Customers who transacted on this date but were first seen before it (repeat). */
    @Query("SELECT COUNT(*) FROM customer_profiles WHERE last_seen_date = :date AND first_seen_date != :date")
    suspend fun countReturningCustomersForDate(date: String): Int
}
