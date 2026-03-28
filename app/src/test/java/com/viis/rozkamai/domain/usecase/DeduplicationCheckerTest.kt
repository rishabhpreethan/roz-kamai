package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeduplicationCheckerTest : BaseUnitTest() {

    private lateinit var eventRepository: EventRepository
    private lateinit var checker: DeduplicationChecker

    private val now = System.currentTimeMillis()

    private fun makeTransaction(
        amount: Double = 100.0,
        type: TransactionType = TransactionType.CREDIT,
        source: PaymentSource = PaymentSource.GPAY,
        upiHandleHash: String? = "abc123hash",
        timestamp: Long = now,
    ) = ParsedTransaction(
        amount = amount,
        type = type,
        source = source,
        upiHandleHash = upiHandleHash,
        merchantNameHash = null,
        referenceId = null,
        timestamp = timestamp,
        rawSenderMasked = "GPAY***",
    )

    private fun makeEvent(amount: Double, type: TransactionType, source: PaymentSource, upiHash: String?) =
        EventEntity(
            eventId = "evt-${System.nanoTime()}",
            eventType = "TransactionDetected",
            timestamp = now,
            payload = buildString {
                append("""{"amount":$amount""")
                append(""","type":"$type"""")
                append(""","source":"$source"""")
                upiHash?.let { append(""","upi_handle_hash":"$it"""") }
                append("}")
            },
        )

    @Before
    override fun setUp() {
        super.setUp()
        eventRepository = mockk()
        checker = DeduplicationChecker(eventRepository)
    }

    @Test
    fun `no recent events returns false (not duplicate)`() = runTest {
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns emptyList()
        assertFalse(checker.isDuplicate(makeTransaction()))
    }

    @Test
    fun `same amount, type, and UPI hash in window is duplicate`() = runTest {
        val tx = makeTransaction(amount = 100.0, type = TransactionType.CREDIT, upiHandleHash = "abc123hash")
        val event = makeEvent(100.0, TransactionType.CREDIT, PaymentSource.GPAY, "abc123hash")
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(event)
        assertTrue(checker.isDuplicate(tx))
    }

    @Test
    fun `same amount but different type is not duplicate`() = runTest {
        val tx = makeTransaction(amount = 100.0, type = TransactionType.CREDIT, upiHandleHash = "abc123hash")
        val event = makeEvent(100.0, TransactionType.DEBIT, PaymentSource.GPAY, "abc123hash")
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(event)
        assertFalse(checker.isDuplicate(tx))
    }

    @Test
    fun `same amount and type but different UPI hash is not duplicate`() = runTest {
        val tx = makeTransaction(amount = 100.0, type = TransactionType.CREDIT, upiHandleHash = "abc123hash")
        val event = makeEvent(100.0, TransactionType.CREDIT, PaymentSource.GPAY, "differenthash")
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(event)
        assertFalse(checker.isDuplicate(tx))
    }

    @Test
    fun `different amount with same UPI hash is not duplicate`() = runTest {
        val tx = makeTransaction(amount = 100.0, upiHandleHash = "abc123hash")
        val event = makeEvent(200.0, TransactionType.CREDIT, PaymentSource.GPAY, "abc123hash")
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(event)
        assertFalse(checker.isDuplicate(tx))
    }

    @Test
    fun `no UPI hash falls back to source match`() = runTest {
        val tx = makeTransaction(amount = 500.0, source = PaymentSource.SBI, upiHandleHash = null)
        val event = makeEvent(500.0, TransactionType.CREDIT, PaymentSource.SBI, null)
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(event)
        assertTrue(checker.isDuplicate(tx))
    }

    @Test
    fun `dedup window is 5 minutes`() {
        assertEquals(5 * 60 * 1000L, DeduplicationChecker.DEDUP_WINDOW_MS)
    }
}
