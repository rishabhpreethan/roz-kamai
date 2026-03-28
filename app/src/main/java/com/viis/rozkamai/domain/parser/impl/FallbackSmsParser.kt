package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Heuristic fallback parser — last resort after all specific parsers have failed.
 * Attempts to extract an amount and transaction type from any SMS body.
 * Lower accuracy than specific parsers — only used when no other parser matches.
 */
@Singleton
class FallbackSmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.UNKNOWN
    override val priority = 100 // lowest priority — runs last

    // Generic amount pattern: Rs/INR followed by a number
    private val amountRegex = Regex(
        """(?:Rs|INR|₹)\.?\s*([\d,]+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )

    private val creditKeywords = listOf("credited", "received", "credit", "added", "deposited")
    private val debitKeywords = listOf("debited", "deducted", "debit", "withdrawn", "paid", "sent")
    private val failedKeywords = listOf("failed", "declined", "rejected", "unsuccessful", "could not")

    // Only attempt if sender looks financial (already filtered by SmsSenderFilter upstream)
    override fun canParse(sender: String, body: String): Boolean = true

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        val bodyLower = body.lowercase()

        if (failedKeywords.any { bodyLower.contains(it) }) return null

        val amountMatch = amountRegex.find(body) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val type = when {
            creditKeywords.any { bodyLower.contains(it) } -> TransactionType.CREDIT
            debitKeywords.any { bodyLower.contains(it) } -> TransactionType.DEBIT
            else -> return null // can't determine direction — skip
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            source = PaymentSource.UPI,
            upiHandleHash = null,
            merchantNameHash = null,
            referenceId = null,
            timestamp = receivedAt,
            rawSenderMasked = "${sender.take(4)}***",
        )
    }
}
