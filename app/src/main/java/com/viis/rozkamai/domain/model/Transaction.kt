package com.viis.rozkamai.domain.model

import com.viis.rozkamai.domain.event.PaymentSource

data class Transaction(
    val id: String,
    val eventId: String,
    val amount: Double,
    val timestamp: Long,
    val senderName: String?,
    val upiHandle: String?,
    val source: PaymentSource,
    val referenceId: String?,
    val status: TransactionStatus
)

enum class TransactionStatus {
    SUCCESS, FAILED, PENDING
}
