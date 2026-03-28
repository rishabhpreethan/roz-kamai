package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import com.viis.rozkamai.util.HashUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhonePeSmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.PHONEPE
    override val priority = 20

    // "Your PhonePe A/c XXXX is credited with Rs 200 by user@ybl"
    private val creditByRegex = Regex(
        """credited\s+with\s+Rs\.?\s*([\d,]+(?:\.\d+)?)\s+by\s+(\S+@\S+)""",
        RegexOption.IGNORE_CASE,
    )
    // "Money received! Rs.300.00 received from buyer@axl on PhonePe"
    private val receivedFromRegex = Regex(
        """Rs\.?\s*([\d,]+(?:\.\d+)?)\s+received\s+from\s+(\S+@\S+)""",
        RegexOption.IGNORE_CASE,
    )
    // "Rs 150 debited from your PhonePe A/c XXXX to merchant@ybl"
    private val debitRegex = Regex(
        """Rs\.?\s*([\d,]+(?:\.\d+)?)\s+debited\s+from\s+your\s+PhonePe\s+A/c\s+\S+\s+to\s+(\S+@\S+)""",
        RegexOption.IGNORE_CASE,
    )
    private val refRegex = Regex("""(?:UPI\s+)?Ref(?:erence)?[:\s]+(\S+)""", RegexOption.IGNORE_CASE)

    override fun canParse(sender: String, body: String): Boolean =
        sender.contains("PhonePe", ignoreCase = true) ||
            body.contains("PhonePe", ignoreCase = true)

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        if (body.contains("failed", ignoreCase = true) ||
            body.contains("declined", ignoreCase = true)
        ) return null

        val creditBy = creditByRegex.find(body)
        if (creditBy != null) {
            val amount = creditBy.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                type = TransactionType.CREDIT,
                source = PaymentSource.PHONEPE,
                upiHandleHash = HashUtils.sha256(creditBy.groupValues[2].lowercase()),
                merchantNameHash = null,
                referenceId = refRegex.find(body)?.groupValues?.get(1),
                timestamp = receivedAt,
                rawSenderMasked = "${sender.take(4)}***",
            )
        }

        val receivedFrom = receivedFromRegex.find(body)
        if (receivedFrom != null) {
            val amount = receivedFrom.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                type = TransactionType.CREDIT,
                source = PaymentSource.PHONEPE,
                upiHandleHash = HashUtils.sha256(receivedFrom.groupValues[2].lowercase()),
                merchantNameHash = null,
                referenceId = refRegex.find(body)?.groupValues?.get(1),
                timestamp = receivedAt,
                rawSenderMasked = "${sender.take(4)}***",
            )
        }

        val debit = debitRegex.find(body)
        if (debit != null) {
            val amount = debit.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                type = TransactionType.DEBIT,
                source = PaymentSource.PHONEPE,
                upiHandleHash = HashUtils.sha256(debit.groupValues[2].lowercase()),
                merchantNameHash = null,
                referenceId = refRegex.find(body)?.groupValues?.get(1),
                timestamp = receivedAt,
                rawSenderMasked = "${sender.take(4)}***",
            )
        }

        return null
    }
}
