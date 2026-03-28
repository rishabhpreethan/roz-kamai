package com.viis.rozkamai.domain.parser

import com.viis.rozkamai.domain.model.ParsedTransaction
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds all registered SMS parsers and dispatches to them in priority order.
 * Pure orchestration — no event logic, no I/O.
 */
@Singleton
class ParserRegistry @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards SmsParser>,
) {
    private val sortedParsers: List<SmsParser> by lazy {
        parsers.sortedBy { it.priority }
    }

    /**
     * Tries parsers in priority order.
     * Returns the first successful parse result, or null if no parser succeeded.
     */
    fun parse(sender: String, body: String, receivedAt: Long): ParsedTransaction? {
        val parser = findParser(sender, body) ?: return null
        return runCatching { parser.parse(sender, body, receivedAt) }
            .onFailure { Timber.e("Parser ${parser.source} threw unexpectedly: ${it.javaClass.simpleName}") }
            .getOrNull()
    }

    /**
     * Returns the highest-priority parser that claims it can handle this sender/body.
     */
    fun findParser(sender: String, body: String): SmsParser? =
        sortedParsers.firstOrNull { it.canParse(sender, body) }
}
