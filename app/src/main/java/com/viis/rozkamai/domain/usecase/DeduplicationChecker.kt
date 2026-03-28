package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.model.ParsedTransaction
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether a parsed transaction is a duplicate of one already recorded.
 *
 * Duplicate criteria (per decision D-203):
 *   - Same amount (exact match)
 *   - Same transaction type (CREDIT/DEBIT)
 *   - Same UPI handle hash (when available) OR same masked sender
 *   - Within the 5-minute deduplication window
 *
 * This is a best-effort check — false negatives (missed duplicates) are safer
 * than false positives (discarding real transactions).
 */
@Singleton
class DeduplicationChecker @Inject constructor(
    private val eventRepository: EventRepository,
) {
    companion object {
        const val DEDUP_WINDOW_MS = 5 * 60 * 1000L // 5 minutes, per decision D-203
    }

    suspend fun isDuplicate(transaction: ParsedTransaction): Boolean {
        val windowStart = transaction.timestamp - DEDUP_WINDOW_MS
        val windowEnd = transaction.timestamp + DEDUP_WINDOW_MS

        val recentEvents = eventRepository.getTransactionDetectedInWindow(windowStart, windowEnd)

        val duplicate = recentEvents.any { event ->
            matchesTransaction(event.payload, transaction)
        }

        if (duplicate) {
            Timber.d("DeduplicationChecker: duplicate detected for amount=${transaction.amount}, type=${transaction.type}")
        }

        return duplicate
    }

    /**
     * Checks if a TransactionDetected event payload matches the candidate transaction.
     * Payload is JSON — we do simple string checks rather than full JSON parsing
     * to avoid an extra Moshi dependency here.
     */
    private fun matchesTransaction(payload: String, tx: ParsedTransaction): Boolean {
        // Must match amount exactly
        if (!payload.contains("\"amount\":${tx.amount}")) return false

        // Must match type
        if (!payload.contains("\"type\":\"${tx.type}\"")) return false

        // If we have a UPI handle hash, use it for precise matching
        if (tx.upiHandleHash != null) {
            return payload.contains("\"upi_handle_hash\":\"${tx.upiHandleHash}\"")
        }

        // Otherwise match on source (less precise, but bank SMS rarely duplicates in 5 min)
        return payload.contains("\"source\":\"${tx.source}\"")
    }
}
