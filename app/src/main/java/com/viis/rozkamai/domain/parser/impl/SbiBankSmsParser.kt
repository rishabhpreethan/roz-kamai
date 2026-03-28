package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SbiBankSmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.SBI
    override val priority = 40

    // "Your A/c XXXX1234 is credited by Rs 500 on 01/01/25. Balance Rs 10500"
    private val creditRegex = Regex(
        """(?:A/c|A/C|account)\s+\S+\s+is?\s+credited\s+(?:by|with)\s+(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )
    // "SBI: INR 1000.00 credited to A/C XXXX"
    private val creditRegex2 = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+credited\s+to\s+(?:A/c|A/C)""",
        RegexOption.IGNORE_CASE,
    )
    // "Your A/c XXXX1234 is debited by Rs 200"
    private val debitRegex = Regex(
        """(?:A/c|A/C|account)\s+\S+\s+is?\s+debited\s+(?:by|with)\s+(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )

    override fun canParse(sender: String, body: String): Boolean =
        sender.contains("SBI", ignoreCase = true) ||
            sender.contains("SBIUPI", ignoreCase = true)

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        if (body.contains("failed", ignoreCase = true) ||
            body.contains("declined", ignoreCase = true)
        ) return null

        val credit = creditRegex.find(body) ?: creditRegex2.find(body)
        if (credit != null) {
            val amount = credit.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                type = TransactionType.CREDIT,
                source = PaymentSource.SBI,
                upiHandleHash = null,
                merchantNameHash = null,
                referenceId = null,
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
                source = PaymentSource.SBI,
                upiHandleHash = null,
                merchantNameHash = null,
                referenceId = null,
                timestamp = receivedAt,
                rawSenderMasked = "${sender.take(4)}***",
            )
        }

        return null
    }
}
