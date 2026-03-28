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
}
