package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AxisBankSmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.AXIS
    override val priority = 70

    // "Axis Bank: Rs.600.00 credited to your A/c XXXX on 01Jan25 by UPI"
    private val creditRegex = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+credited\s+to\s+(?:your\s+)?(?:Axis|A/c|A/C)""",
        RegexOption.IGNORE_CASE,
    )
    // "INR 350 credited to Axis Bank A/C XXXX via UPI"
    private val creditRegex2 = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+credited\s+to\s+Axis\s+Bank""",
        RegexOption.IGNORE_CASE,
    )
    private val debitRegex = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+debited\s+(?:from|to)\s+(?:your\s+)?(?:Axis|A/c)""",
        RegexOption.IGNORE_CASE,
    )

    override fun canParse(sender: String, body: String): Boolean =
        sender.contains("AXISBK", ignoreCase = true) ||
            sender.contains("AXIS", ignoreCase = true)

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
                source = PaymentSource.AXIS,
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
                source = PaymentSource.AXIS,
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
