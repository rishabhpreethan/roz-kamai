package com.viis.rozkamai.domain.model

import com.viis.rozkamai.domain.event.PaymentSource

/**
 * Result of a successful SMS parse.
 * All PII is hashed before this object is created — no raw UPI handles or names stored.
 */
data class ParsedTransaction(
    val amount: Double,
    val type: TransactionType,
    val source: PaymentSource,
    val upiHandleHash: String?,   // SHA-256 of the UPI handle, never raw
    val merchantNameHash: String?, // SHA-256 of merchant name, never raw
    val referenceId: String?,
    val timestamp: Long,
    val rawSenderMasked: String,   // first 4 chars + ***
)

enum class TransactionType { CREDIT, DEBIT }
