package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HdfcBankSmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.HDFC
    override val priority = 50

    // "Rs 300 credited to HDFC Bank A/c XX1234 by NEFT/UPI"
    private val creditRegex = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+credited\s+to\s+HDFC""",
        RegexOption.IGNORE_CASE,
    )
    // "Money received! Rs 750.00 to HDFC A/c XXXX on ... via UPI"
    private val creditRegex2 = Regex(
        """(?:Money received[!.]?\s+)?(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+(?:to|received)\s+(?:HDFC|your\s+HDFC)""",
        RegexOption.IGNORE_CASE,
    )
    private val debitRegex = Regex(
        """(?:Rs|INR)\.?\s*([\d,]+(?:\.\d+)?)\s+debited\s+(?:from|to)\s+(?:HDFC|your\s+HDFC)""",
        RegexOption.IGNORE_CASE,
    )

    override fun canParse(sender: String, body: String): Boolean =
        sender.contains("HDFCBK", ignoreCase = true) ||
            sender.contains("HDFC", ignoreCase = true)

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
                source = PaymentSource.HDFC,
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
                source = PaymentSource.HDFC,
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
