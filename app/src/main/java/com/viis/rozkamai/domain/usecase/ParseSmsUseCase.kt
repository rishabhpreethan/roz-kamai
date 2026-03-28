package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.parser.ParseResult
import com.viis.rozkamai.domain.parser.ParserRegistry
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates SMS parsing and event production.
 * This is the only place that writes parsing-related events to the EventStore.
 */
@Singleton
class ParseSmsUseCase @Inject constructor(
    private val registry: ParserRegistry,
    private val eventRepository: EventRepository,
) {
    suspend fun execute(sender: String, body: String, receivedAt: Long): ParseResult {
        val senderMasked = "${sender.take(4)}***"
        val parsed = registry.parse(sender, body, receivedAt)

        return if (parsed != null) {
            val payload = buildTransactionPayload(parsed)
            eventRepository.appendEvent(
                EventEntity(
                    eventId = UUID.randomUUID().toString(),
                    eventType = "TransactionDetected",
                    timestamp = receivedAt,
                    payload = payload,
                    version = 1,
                ),
            )
            Timber.d("TransactionDetected: source=${parsed.source}, type=${parsed.type}")
            ParseResult.Success(transaction = parsed, parserSource = parsed.source)
        } else {
            val reason = "No parser matched sender $senderMasked"
            eventRepository.appendEvent(
                EventEntity(
                    eventId = UUID.randomUUID().toString(),
                    eventType = "ParseFailed",
                    timestamp = receivedAt,
                    payload = """{"sender_masked":"$senderMasked","reason":"no_parser_matched","body_length":${body.length}}""",
                    version = 1,
                ),
            )
            Timber.d("ParseFailed: sender=$senderMasked")
            ParseResult.Failed(reason = reason, senderMasked = senderMasked)
        }
    }

    private fun buildTransactionPayload(parsed: com.viis.rozkamai.domain.model.ParsedTransaction): String {
        // Amounts are safe to store — they are financial data, not PII
        // UPI handles are already hashed by the parser via HashUtils
        return buildString {
            append("""{"amount":${parsed.amount}""")
            append(""","type":"${parsed.type}"""")
            append(""","source":"${parsed.source}"""")
            parsed.upiHandleHash?.let { append(""","upi_handle_hash":"$it"""") }
            parsed.referenceId?.let { append(""","reference_id":"$it"""") }
            append(""","timestamp":${parsed.timestamp}""")
            append("}")
        }
    }
}
