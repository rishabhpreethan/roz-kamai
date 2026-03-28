package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.CustomerProfileDao
import com.viis.rozkamai.data.local.entity.CustomerProfileEntity
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identifies customers from CREDIT transactions that carry a UPI handle hash.
 * Creates a new CustomerProfileEntity on first encounter; increments totals on repeat.
 * Emits a CustomerIdentified event for each call.
 *
 * Covered tasks:
 *   P2-010  Customer identification (UPI handle clustering)
 *   P2-011  Repeat customer detection (transactionCount >= 2)
 *   P2-012  New vs returning classification (isNew flag on CustomerIdentified event)
 *
 * Privacy rules:
 *   - customerId = upiHandleHash (SHA-256) — never the raw UPI handle
 *   - displayName is never populated — field exists in schema but is always null here
 *   - No name or merchant correlation stored in this service
 */
@Singleton
class CustomerIdentificationService @Inject constructor(
    private val customerProfileDao: CustomerProfileDao,
    private val eventRepository: EventRepository,
) {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Identifies the customer behind [transaction] (CREDIT only, requires upiHandleHash).
     * Must be called BEFORE AggregationEngine.aggregate() so the profile is current
     * when the daily new/returning customer counts are computed.
     *
     * @param transactionEventId the TransactionDetected event ID for the CustomerIdentified event link
     */
    suspend fun identify(transaction: ParsedTransaction, transactionEventId: String) {
        // Only track customers from incoming payments — skip DEBITs and anonymous transactions
        if (transaction.type != TransactionType.CREDIT) return
        val hash = transaction.upiHandleHash ?: return

        val date = dateFmt.format(Date(transaction.timestamp))
        val existing = customerProfileDao.getById(hash)
        val isNew = existing == null

        val updatedProfile = if (isNew) {
            CustomerProfileEntity(
                customerId = hash,
                displayName = null,       // never populated — privacy rule
                upiHandleHash = hash,
                firstSeen = transaction.timestamp,
                lastSeen = transaction.timestamp,
                firstSeenDate = date,
                lastSeenDate = date,
                transactionCount = 1,
                totalAmount = transaction.amount,
            )
        } else {
            existing!!.copy(
                lastSeen = transaction.timestamp,
                lastSeenDate = date,
                transactionCount = existing.transactionCount + 1,
                totalAmount = existing.totalAmount + transaction.amount,
                updatedAt = System.currentTimeMillis(),
            )
        }
        customerProfileDao.upsert(updatedProfile)

        appendCustomerIdentifiedEvent(hash, isNew, transactionEventId)
        Timber.d("CustomerIdentificationService: isNew=$isNew count=${updatedProfile.transactionCount}")
    }

    // ─── P2-011, P2-012: Helpers ──────────────────────────────────────────────

    /** True if this customer has made more than one transaction (repeat / returning). */
    fun isRepeatCustomer(profile: CustomerProfileEntity): Boolean =
        profile.transactionCount >= 2

    /** True if this is the customer's first-ever transaction with this merchant. */
    fun isNewCustomer(profile: CustomerProfileEntity): Boolean =
        profile.transactionCount == 1

    // ─── Event production ─────────────────────────────────────────────────────

    private suspend fun appendCustomerIdentifiedEvent(
        customerId: String,
        isNew: Boolean,
        transactionEventId: String,
    ) {
        eventRepository.appendEvent(
            EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = "CustomerIdentified",
                timestamp = System.currentTimeMillis(),
                payload = """{"customer_id":"$customerId","is_new":$isNew,"transaction_event_id":"$transactionEventId"}""",
                version = 1,
            ),
        )
    }
}
