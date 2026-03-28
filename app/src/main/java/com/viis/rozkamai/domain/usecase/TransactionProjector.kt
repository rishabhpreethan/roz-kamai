package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.TransactionEntity
import com.viis.rozkamai.domain.model.ParsedTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Projects a ParsedTransaction into the transactions read model table,
 * then triggers AggregationEngine to recompute all daily/hourly summaries.
 *
 * The read model (transactions table) can be fully rebuilt from the event store.
 * The aggregation projections (daily_summaries, hourly_stats) follow the same rule.
 */
@Singleton
class TransactionProjector @Inject constructor(
    private val transactionDao: TransactionDao,
    private val aggregationEngine: AggregationEngine,
) {
    private val dateBucketFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun project(transaction: ParsedTransaction, eventId: String) {
        val dateBucket = dateBucketFormat.format(Date(transaction.timestamp))
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            amount = transaction.amount,
            type = transaction.type.name,
            source = transaction.source.name,
            timestamp = transaction.timestamp,
            dateBucket = dateBucket,
            rawSenderMasked = transaction.rawSenderMasked,
            upiHandleHash = transaction.upiHandleHash,
            referenceId = transaction.referenceId,
            status = "SUCCESS",
        )
        transactionDao.insert(entity)
        aggregationEngine.aggregate(transaction, dateBucket)
    }
}
