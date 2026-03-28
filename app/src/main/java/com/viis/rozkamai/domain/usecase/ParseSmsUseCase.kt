package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.parser.ParseResult
import com.viis.rozkamai.domain.parser.ParserRegistry
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full SMS processing pipeline:
 *   1. Pre-check for failed transaction language → TransactionFailed event
 *   2. Parse via ParserRegistry → ParseFailed event on no match
 *   3. Deduplication check → DuplicateDetected event
 *   4. Append TransactionDetected event + project to transactions read model
 *
 * Single responsibility: this is the only class that writes parse-related events
 * and triggers the transaction read-model projection.
 */
@Singleton
class ParseSmsUseCase @Inject constructor(
    private val registry: ParserRegistry,
    private val eventRepository: EventRepository,
    private val deduplicationChecker: DeduplicationChecker,
    private val transactionProjector: TransactionProjector,
) {
    suspend fun execute(sender: String, body: String, receivedAt: Long): ParseResult {
        val senderMasked = "${sender.take(4)}***"

        // Step 1 — detect failed transaction before attempting to parse
        if (FailedTransactionDetector.isFailedTransaction(body)) {
            val amount = FailedTransactionDetector.extractAmount(body)
            appendFailedTransactionEvent(senderMasked, amount, receivedAt)
            Timber.d("TransactionFailed detected from $senderMasked")
            return ParseResult.Failed(reason = "transaction_failed_in_sms", senderMasked = senderMasked)
        }

        // Step 2 — parse
        val parsed = registry.parse(sender, body, receivedAt)
            ?: return run {
                appendParseFailedEvent(senderMasked, body.length, receivedAt)
                Timber.d("ParseFailed: no parser matched sender $senderMasked")
                ParseResult.Failed(reason = "no_parser_matched", senderMasked = senderMasked)
            }

        // Step 3 — deduplication
        if (deduplicationChecker.isDuplicate(parsed)) {
            appendDuplicateDetectedEvent(parsed, receivedAt)
            Timber.d("DuplicateDetected: amount=${parsed.amount}, type=${parsed.type}")
            return ParseResult.Duplicate
        }

        // Step 4 — new transaction: append event then project to read model
        val eventId = UUID.randomUUID().toString()
        appendTransactionDetectedEvent(parsed, receivedAt, eventId)
        transactionProjector.project(parsed, eventId)
        Timber.d("TransactionDetected: source=${parsed.source}, type=${parsed.type}")
        return ParseResult.Success(transaction = parsed, parserSource = parsed.source)
    }

    private suspend fun appendTransactionDetectedEvent(
        parsed: ParsedTransaction,
        receivedAt: Long,
        eventId: String,
    ) {
        eventRepository.appendEvent(
            EventEntity(
                eventId = eventId,
                eventType = "TransactionDetected",
                timestamp = receivedAt,
                payload = buildTransactionPayload(parsed),
                version = 1,
            ),
        )
    }

    private suspend fun appendParseFailedEvent(senderMasked: String, bodyLength: Int, receivedAt: Long) {
        eventRepository.appendEvent(
            EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = "ParseFailed",
                timestamp = receivedAt,
                payload = """{"sender_masked":"$senderMasked","reason":"no_parser_matched","body_length":$bodyLength}""",
                version = 1,
            ),
        )
    }

    private suspend fun appendFailedTransactionEvent(senderMasked: String, amount: Double?, receivedAt: Long) {
        val amountField = if (amount != null) ""","amount":$amount""" else ""
        eventRepository.appendEvent(
            EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = "TransactionFailed",
                timestamp = receivedAt,
                payload = """{"sender_masked":"$senderMasked"$amountField}""",
                version = 1,
            ),
        )
    }

    private suspend fun appendDuplicateDetectedEvent(parsed: ParsedTransaction, receivedAt: Long) {
        eventRepository.appendEvent(
            EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = "DuplicateDetected",
                timestamp = receivedAt,
                payload = """{"amount":${parsed.amount},"type":"${parsed.type}","source":"${parsed.source}"}""",
                version = 1,
            ),
        )
    }

    private fun buildTransactionPayload(parsed: ParsedTransaction): String = buildString {
        append("""{"amount":${parsed.amount}""")
        append(""","type":"${parsed.type}"""")
        append(""","source":"${parsed.source}"""")
        parsed.upiHandleHash?.let { append(""","upi_handle_hash":"$it"""") }
        parsed.referenceId?.let { append(""","reference_id":"$it"""") }
        append(""","timestamp":${parsed.timestamp}""")
        append("}")
    }
}
