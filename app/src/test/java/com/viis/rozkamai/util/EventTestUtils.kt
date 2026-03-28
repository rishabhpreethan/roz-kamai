package com.viis.rozkamai.util

import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.domain.event.DomainEvent
import com.viis.rozkamai.domain.event.PaymentSource
import java.util.UUID

/**
 * Utility functions for creating test fixtures related to events.
 * All test data uses fake amounts and anonymized identifiers — no real PII.
 */
object EventTestUtils {

    fun makeEventEntity(
        eventType: String = "SMSReceived",
        payload: String = "{}",
        timestamp: Long = System.currentTimeMillis(),
        version: Int = 1,
    ): EventEntity = EventEntity(
        eventId = UUID.randomUUID().toString(),
        eventType = eventType,
        timestamp = timestamp,
        payload = payload,
        version = version,
    )

    /** Sample GPay SMS for unit tests — anonymized, no real UPI handles or names */
    fun gpayCredit(amount: Double = 100.0): String =
        "Rs. $amount credited to your account by user@okaxis via GPay"

    fun gpayDebit(amount: Double = 50.0): String =
        "Rs. $amount debited from your account to merchant@oksbi via GPay"

    /** Sample PhonePe SMS */
    fun phonepeCredit(amount: Double = 200.0): String =
        "Your PhonePe A/c XXXX is credited with Rs $amount by user@ybl"

    /** Sample Paytm SMS */
    fun paytmCredit(amount: Double = 150.0): String =
        "Received Rs. $amount from Paytm user. Txn ID: TEST${(1000..9999).random()}"

    /** Sample SBI bank SMS */
    fun sbiCredit(amount: Double = 500.0, balance: Double = 10000.0): String =
        "Your A/c XXXX1234 is credited by Rs $amount on ${System.currentTimeMillis()}. Balance Rs $balance"

    /** Sample HDFC bank SMS */
    fun hdfcCredit(amount: Double = 300.0): String =
        "Rs ${amount} credited to HDFC Bank A/c XX1234 by NEFT/UPI on ${System.currentTimeMillis()}"

    /** Failed transaction samples */
    fun failedTransaction(): String =
        "Transaction of Rs. 100.0 failed. Please try again."

    fun insufficientFunds(): String =
        "Transaction declined due to insufficient funds."
}
