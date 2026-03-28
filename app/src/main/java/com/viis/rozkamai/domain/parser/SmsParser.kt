package com.viis.rozkamai.domain.parser

import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction

/**
 * Interface every SMS parser must implement.
 * Parsers are pure functions — no I/O, no coroutines, no side effects.
 * All PII (UPI handles, names) must be hashed via HashUtils before being returned.
 */
interface SmsParser {
    /** Which payment source this parser handles. */
    val source: PaymentSource

    /** Lower number = higher priority in the registry. */
    val priority: Int

    /**
     * Returns true if this parser can attempt to parse this sender/body combination.
     * Should be fast (regex match on sender ID is ideal).
     */
    fun canParse(sender: String, body: String): Boolean

    /**
     * Attempts to parse the SMS. Returns null if parsing fails.
     * Never throws — return null on any unexpected input.
     */
    fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction?
}
