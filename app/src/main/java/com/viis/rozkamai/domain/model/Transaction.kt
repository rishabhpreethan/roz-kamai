package com.viis.rozkamai.domain.model

import com.viis.rozkamai.domain.event.PaymentSource

data class Transaction(
    val id: String,
    val eventId: String,
    val amount: Double,
    val type: String, // TransactionType.name
    val source: PaymentSource,
    val timestamp: Long,
    val dateBucket: String, // "YYYY-MM-DD"
    val rawSenderMasked: String, // first 4 chars + "***"
    val upiHandleHash: String?,  // SHA-256 — never raw handle
    val referenceId: String?,
    val status: TransactionStatus,
)

enum class TransactionStatus {
    SUCCESS, FAILED, PENDING
}
