package com.viis.rozkamai.domain.parser

import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.usecase.ParseSmsUseCase
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParseSmsUseCaseTest : BaseUnitTest() {

    private lateinit var registry: ParserRegistry
    private lateinit var eventRepository: EventRepository
    private lateinit var useCase: ParseSmsUseCase

    private val sender = "GPAY"
    private val body = "Rs. 100 credited via GPay"
    private val receivedAt = 1000L

    private fun makeTransaction() = ParsedTransaction(
        amount = 100.0,
        type = TransactionType.CREDIT,
        source = PaymentSource.UPI,
        upiHandleHash = null,
        merchantNameHash = null,
        referenceId = "REF001",
        timestamp = receivedAt,
        rawSenderMasked = "GPAY***",
    )

    @Before
    override fun setUp() {
        super.setUp()
        registry = mockk()
        eventRepository = mockk(relaxed = true)
        useCase = ParseSmsUseCase(registry, eventRepository)
    }

    @Test
    fun `successful parse appends TransactionDetected event`() = runTest {
        every { registry.parse(sender, body, receivedAt) } returns makeTransaction()

        useCase.execute(sender, body, receivedAt)

        coVerify {
            eventRepository.appendEvent(match { event: EventEntity ->
                event.eventType == "TransactionDetected"
            })
        }
    }

    @Test
    fun `successful parse returns Success`() = runTest {
        every { registry.parse(sender, body, receivedAt) } returns makeTransaction()

        val result = useCase.execute(sender, body, receivedAt)

        assertTrue(result is ParseResult.Success)
    }

    @Test
    fun `failed parse appends ParseFailed event`() = runTest {
        every { registry.parse(sender, body, receivedAt) } returns null

        useCase.execute(sender, body, receivedAt)

        coVerify {
            eventRepository.appendEvent(match { event: EventEntity ->
                event.eventType == "ParseFailed"
            })
        }
    }

    @Test
    fun `failed parse returns Failed`() = runTest {
        every { registry.parse(sender, body, receivedAt) } returns null

        val result = useCase.execute(sender, body, receivedAt)

        assertTrue(result is ParseResult.Failed)
    }

    @Test
    fun `TransactionDetected payload contains amount`() = runTest {
        every { registry.parse(sender, body, receivedAt) } returns makeTransaction()

        useCase.execute(sender, body, receivedAt)

        coVerify {
            eventRepository.appendEvent(match { event: EventEntity ->
                event.payload.contains("100.0")
            })
        }
    }

    @Test
    fun `ParseFailed payload contains masked sender not raw sender`() = runTest {
        every { registry.parse(sender, body, receivedAt) } returns null

        useCase.execute(sender, body, receivedAt)

        coVerify {
            eventRepository.appendEvent(match { event: EventEntity ->
                event.payload.contains("GPAY***") && !event.payload.contains(sender + "\"")
            })
        }
    }

    @Test
    fun `success result carries correct parserSource`() = runTest {
        every { registry.parse(sender, body, receivedAt) } returns makeTransaction()

        val result = useCase.execute(sender, body, receivedAt) as ParseResult.Success

        assertTrue(result.parserSource == PaymentSource.UPI)
    }
}
