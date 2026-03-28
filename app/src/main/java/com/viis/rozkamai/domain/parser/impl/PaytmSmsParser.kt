package com.viis.rozkamai.domain.parser.impl

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaytmSmsParser @Inject constructor() : SmsParser {

    override val source = PaymentSource.PAYTM
    override val priority = 30

    // "Received Rs. 150 from Paytm user." / "Paytm: Rs.600 received from buyer."
    private val creditRegex = Regex(
        """(?:Received\s+Rs\.?\s*([\d,]+(?:\.\d+)?)\s+from\s+Paytm|Paytm[:\s]+Rs\.?\s*([\d,]+(?:\.\d+)?)\s+received|received\s+Rs\.?\s*([\d,]+(?:\.\d+)?)\s+in\s+your\s+Paytm)""",
        RegexOption.IGNORE_CASE,
    )
    private val txnIdRegex = Regex("""Txn\s+ID[:\s]+(\S+)""", RegexOption.IGNORE_CASE)

    override fun canParse(sender: String, body: String): Boolean =
        sender.contains("PAYTM", ignoreCase = true) ||
            sender.contains("PYTMUPI", ignoreCase = true) ||
            body.contains("Paytm", ignoreCase = true)

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        if (body.contains("failed", ignoreCase = true) ||
            body.contains("declined", ignoreCase = true)
        ) return null

        val match = creditRegex.find(body) ?: return null
        // Pick the first non-empty group (different patterns capture in different groups)
        val amountStr = (match.groupValues[1].ifEmpty { null }
            ?: match.groupValues[2].ifEmpty { null }
            ?: match.groupValues[3].ifEmpty { null })
            ?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null

        return ParsedTransaction(
            amount = amount,
            type = TransactionType.CREDIT,
            source = PaymentSource.PAYTM,
            upiHandleHash = null, // Paytm SMS rarely exposes UPI handle in body
            merchantNameHash = null,
            referenceId = txnIdRegex.find(body)?.groupValues?.get(1),
            timestamp = receivedAt,
            rawSenderMasked = "${sender.take(4)}***",
        )
    }
}
