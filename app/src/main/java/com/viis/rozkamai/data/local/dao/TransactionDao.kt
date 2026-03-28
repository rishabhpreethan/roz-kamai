package com.viis.rozkamai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.viis.rozkamai.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE date_bucket = :date AND status = 'SUCCESS' ORDER BY timestamp ASC")
    fun observeTodayTransactions(date: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date_bucket = :date ORDER BY timestamp ASC")
    suspend fun getTransactionsByDate(date: String): List<TransactionEntity>

    @Query("SELECT SUM(amount) FROM transactions WHERE date_bucket = :date AND status = 'SUCCESS'")
    suspend fun getTotalIncomeForDate(date: String): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE date_bucket = :date AND status = 'SUCCESS'")
    suspend fun getTransactionCountForDate(date: String): Int

    @Query("""
        SELECT * FROM transactions
        WHERE date_bucket = :date AND status = 'SUCCESS'
        AND (upi_handle = :upiHandle OR (upi_handle IS NULL AND sender_name = :senderName))
        AND timestamp BETWEEN :fromTimestamp AND :toTimestamp
        LIMIT 1
    """)
    suspend fun findDuplicate(
        date: String,
        upiHandle: String?,
        senderName: String?,
        fromTimestamp: Long,
        toTimestamp: Long
    ): TransactionEntity?
}
