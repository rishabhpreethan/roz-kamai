package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IciciBankSmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.ICICI
    override val priority = 60

    // "ICICI Bank: Rs 450.00 credited to A/c XX1234 on 01-Jan-25 by UPI"
    private val creditRegex = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+credited\s+to\s+(?:your\s+)?(?:ICICI|A/c|A/C)""",
        RegexOption.IGNORE_CASE,
    )
    // "Rs 200 has been credited to your ICICI Bank A/c XXXX via UPI"
    private val creditRegex2 = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+has\s+been\s+credited\s+to\s+your\s+ICICI""",
        RegexOption.IGNORE_CASE,
    )
    private val debitRegex = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+(?:has\s+been\s+)?debited\s+(?:from|to)\s+(?:your\s+)?(?:ICICI|A/c)""",
        RegexOption.IGNORE_CASE,
    )

    override fun canParse(sender: String, body: String): Boolean =
        sender.contains("ICICIB", ignoreCase = true) ||
            sender.contains("ICICI", ignoreCase = true)

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
                source = PaymentSource.ICICI,
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
                source = PaymentSource.ICICI,
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
