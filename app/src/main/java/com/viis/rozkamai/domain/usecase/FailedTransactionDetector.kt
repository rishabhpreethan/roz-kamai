package com.viis.rozkamai.domain.usecase

/**
 * Detects whether an SMS body describes a payment failure before parsing is attempted.
 *
 * This runs as a pre-filter in ParseSmsUseCase. If a failure is detected, we produce
 * a TransactionFailed event instead of attempting to parse and getting a ParseFailed event.
 * The distinction matters for analytics: ParseFailed = unknown format, TransactionFailed = payment failed.
 */
object FailedTransactionDetector {

    private val failurePatterns = listOf(
        Regex("""transaction.{0,20}fail""", RegexOption.IGNORE_CASE),
        Regex("""payment.{0,20}fail""", RegexOption.IGNORE_CASE),
        Regex("""transfer.{0,20}fail""", RegexOption.IGNORE_CASE),
        Regex("""could not be processed""", RegexOption.IGNORE_CASE),
        Regex("""insufficient funds""", RegexOption.IGNORE_CASE),
        Regex("""insufficient balance""", RegexOption.IGNORE_CASE),
        Regex("""transaction.{0,20}declin""", RegexOption.IGNORE_CASE),
        Regex("""payment.{0,20}declin""", RegexOption.IGNORE_CASE),
        Regex("""transaction.{0,20}reject""", RegexOption.IGNORE_CASE),
        Regex("""transaction.{0,20}unsuccessful""", RegexOption.IGNORE_CASE),
        Regex("""debit.{0,20}fail""", RegexOption.IGNORE_CASE),
        Regex("""not processed""", RegexOption.IGNORE_CASE),
    )

    // Regex to extract amount from a failed SMS (best-effort — may return null)
    private val amountRegex = Regex(
        """(?:Rs|INR|₹)\.?\s*([\d,]+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )

    fun isFailedTransaction(body: String): Boolean =
        failurePatterns.any { it.containsMatchIn(body) }

    /**
     * Attempts to extract the amount from a failure SMS for event payload.
     * Returns null if no amount found — that's fine, we still record the failure event.
     */
    fun extractAmount(body: String): Double? =
        amountRegex.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
}
