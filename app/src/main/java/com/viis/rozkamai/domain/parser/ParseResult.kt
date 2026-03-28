package com.viis.rozkamai.domain.parser

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction

sealed class ParseResult {
    data class Success(
        val transaction: ParsedTransaction,
        val parserSource: PaymentSource,
    ) : ParseResult()

    data class Failed(
        val reason: String,
        val senderMasked: String, // first 4 chars + ***, never full sender
    ) : ParseResult()

    object Duplicate : ParseResult()
}
