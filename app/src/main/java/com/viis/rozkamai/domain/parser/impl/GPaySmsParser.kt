package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import com.viis.rozkamai.util.HashUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GPaySmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.GPAY
    override val priority = 10

    private val creditRegex = Regex(
        """Rs\.?\s*([\d,]+(?:\.\d+)?)\s+credited\s+to\s+your\s+account\s+by\s+(\S+@\S+)\s+via\s+GPay""",
        RegexOption.IGNORE_CASE,
    )
    private val debitRegex = Regex(
        """Rs\.?\s*([\d,]+(?:\.\d+)?)\s+debited\s+from\s+your\s+account\s+to\s+(\S+@\S+)\s+via\s+GPay""",
        RegexOption.IGNORE_CASE,
    )
    private val refRegex = Regex("""Ref(?:erence)?[:\s]+(\S+)""", RegexOption.IGNORE_CASE)

    override fun canParse(sender: String, body: String): Boolean =
        sender.contains("GPAY", ignoreCase = true) ||
            body.contains("via GPay", ignoreCase = true)

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        if (body.contains("failed", ignoreCase = true) ||
            body.contains("declined", ignoreCase = true)
        ) return null

        val credit = creditRegex.find(body)
        if (credit != null) {
            val amount = credit.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val upiHandle = credit.groupValues[2]
            return ParsedTransaction(
                amount = amount,
                type = TransactionType.CREDIT,
                source = PaymentSource.GPAY,
                upiHandleHash = HashUtils.sha256(upiHandle.lowercase()),
                merchantNameHash = null,
                referenceId = refRegex.find(body)?.groupValues?.get(1),
                timestamp = receivedAt,
                rawSenderMasked = "${sender.take(4)}***",
            )
        }

        val debit = debitRegex.find(body)
        if (debit != null) {
            val amount = debit.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val upiHandle = debit.groupValues[2]
            return ParsedTransaction(
                amount = amount,
                type = TransactionType.DEBIT,
                source = PaymentSource.GPAY,
                upiHandleHash = HashUtils.sha256(upiHandle.lowercase()),
                merchantNameHash = null,
                referenceId = refRegex.find(body)?.groupValues?.get(1),
                timestamp = receivedAt,
                rawSenderMasked = "${sender.take(4)}***",
            )
        }

        return null
    }
}
