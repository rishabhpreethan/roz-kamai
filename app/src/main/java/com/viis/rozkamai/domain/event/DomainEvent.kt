package com.viis.rozkamai.domain.event

import com.squareup.moshi.JsonClass

/**
 * All domain events in the VIIS system.
 * Events are immutable facts — they are never modified after creation.
 * They are the source of truth from which all read models are derived.
 */
sealed class DomainEvent {

    // ─── Source Events ────────────────────────────────────────────────────────

    data class SmsReceived(
        val rawBody: String,
        val senderId: String,
        val receivedAt: Long
    ) : DomainEvent()

    // ─── Transaction Events ───────────────────────────────────────────────────

    data class TransactionDetected(
        val amount: Double,
        val senderName: String?,
        val upiHandle: String?,
        val source: PaymentSource,
        val referenceId: String?,
        val receivedAt: Long,
        val smsEventId: String
    ) : DomainEvent()

    data class TransactionFailed(
        val amount: Double?,
        val reason: String,
        val receivedAt: Long,
        val smsEventId: String
    ) : DomainEvent()

    data class DuplicateDetected(
        val originalEventId: String,
        val duplicateSmsBody: String,
        val receivedAt: Long
    ) : DomainEvent()

    data class ParseFailed(
        val rawBody: String,
        val senderId: String,
        val reason: String,
        val receivedAt: Long
    ) : DomainEvent()

    // ─── Customer Events ──────────────────────────────────────────────────────

    data class CustomerIdentified(
        val customerId: String,
        val displayName: String?,
        val upiHandle: String?,
        val isNew: Boolean,
        val transactionEventId: String
    ) : DomainEvent()

    // ─── Insight Events ───────────────────────────────────────────────────────

    data class InsightGenerated(
        val insightType: InsightType,
        val value: Double,
        val comparison: Double?,
        val computedAt: Long
    ) : DomainEvent()

    data class DailySummaryComputed(
        val date: String,
        val totalIncome: Double,
        val transactionCount: Int,
        val avgTransactionValue: Double,
        val peakHour: Int?,
        val firstTxnTime: Long?,
        val lastTxnTime: Long?,
        val expectedIncome: Double?,
        val runRateProjection: Double?,
        val consistencyScore: Double?,
        val computedAt: Long
    ) : DomainEvent()

    // ─── Alert Events ─────────────────────────────────────────────────────────

    data class IdleDetected(
        val gapMinutes: Long,
        val lastTxnTime: Long,
        val threshold: Long,
        val detectedAt: Long
    ) : DomainEvent()

    data class UnderperformanceDetected(
        val expected: Double,
        val actual: Double,
        val deficit: Double,
        val detectedAt: Long
    ) : DomainEvent()

    data class NotificationSent(
        val type: NotificationType,
        val title: String,
        val body: String,
        val sentAt: Long
    ) : DomainEvent()

    // ─── Lifecycle Events ─────────────────────────────────────────────────────

    data class OnboardingCompleted(
        val language: String,
        val completedAt: Long
    ) : DomainEvent()

    data class PermissionGranted(
        val permission: String,
        val grantedAt: Long
    ) : DomainEvent()

    data class PermissionDenied(
        val permission: String,
        val isPermanent: Boolean,
        val deniedAt: Long
    ) : DomainEvent()
}

enum class PaymentSource {
    GPAY, PHONEPE, PAYTM,          // UPI apps
    SBI, HDFC, ICICI, AXIS,        // Bank SMS
    UPI, NEFT, IMPS, BANK,         // Generic / fallback
    UNKNOWN
}

enum class InsightType {
    EXPECTED_EARNINGS, RUN_RATE, SLOW_HOUR,
    TRANSACTION_COUNT, AVG_SALE_VALUE, PEAK_HOUR,
    IDLE_TIME, FIRST_LAST_SALE, WEEKLY_TREND,
    BEST_WORST_DAY, PAYMENT_SPLIT, CONSISTENCY_SCORE
}

enum class NotificationType {
    EOD_SUMMARY, MIDDAY_ALERT, INACTIVITY_ALERT, WEEKLY_SUMMARY
}
