package com.viis.rozkamai.domain.parser

import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.domain.parser.impl.AxisBankSmsParser
import com.viis.rozkamai.domain.parser.impl.FallbackSmsParser
import com.viis.rozkamai.domain.parser.impl.GPaySmsParser
import com.viis.rozkamai.domain.parser.impl.HdfcBankSmsParser
import com.viis.rozkamai.domain.parser.impl.IciciBankSmsParser
import com.viis.rozkamai.domain.parser.impl.PaytmSmsParser
import com.viis.rozkamai.domain.parser.impl.PhonePeSmsParser
import com.viis.rozkamai.domain.parser.impl.SbiBankSmsParser
import com.viis.rozkamai.domain.usecase.DeduplicationChecker
import com.viis.rozkamai.domain.usecase.ParseSmsUseCase
import com.viis.rozkamai.domain.usecase.TransactionProjector
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test for the full SMS processing pipeline.
 *
 * Uses REAL instances of every parser, ParserRegistry, DeduplicationChecker,
 * and TransactionProjector. Only the persistence layer (EventRepository,
 * TransactionDao) is mocked to avoid a Room database dependency in JVM tests.
 *
 * Coverage:
 *   - End-to-end happy paths for every payment source
 *   - Failed transaction pre-check fires before parser is invoked
 *   - Deduplication window correctly suppresses a second identical transaction
 *   - Unknown SMS correctly produces ParseFailed event
 *   - Correct events appended at each step
 */
class SmsPipelineIntegrationTest : BaseUnitTest() {

    // ── Real components ───────────────────────────────────────────────────────
    private lateinit var registry: ParserRegistry
    private lateinit var deduplicationChecker: DeduplicationChecker
    private lateinit var transactionProjector: TransactionProjector

    // ── Mocked persistence ────────────────────────────────────────────────────
    private lateinit var eventRepository: EventRepository
    private lateinit var transactionDao: TransactionDao

    private lateinit var useCase: ParseSmsUseCase

    private val receivedAt = 1_000_000L

    @Before
    override fun setUp() {
        super.setUp()

        eventRepository = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)

        // Default: no recent events (not a duplicate)
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns emptyList()

        registry = ParserRegistry(
            setOf(
                GPaySmsParser(),
                PhonePeSmsParser(),
                PaytmSmsParser(),
                SbiBankSmsParser(),
                HdfcBankSmsParser(),
                IciciBankSmsParser(),
                AxisBankSmsParser(),
                FallbackSmsParser(),
            ),
        )

        deduplicationChecker = DeduplicationChecker(eventRepository)
        transactionProjector = TransactionProjector(transactionDao)
        useCase = ParseSmsUseCase(registry, eventRepository, deduplicationChecker, transactionProjector)
    }

    // ── Happy path — one transaction per payment source ───────────────────────

    @Test
    fun `GPay credit SMS produces TransactionDetected event and Success result`() = runTest {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay. Ref: TEST001"
        val result = useCase.execute("GPAY", body, receivedAt)

        assertTrue(result is ParseResult.Success)
        assertEquals(PaymentSource.GPAY, (result as ParseResult.Success).parserSource)
        coVerify { eventRepository.appendEvent(match { it.eventType == "TransactionDetected" }) }
        coVerify { transactionDao.insert(match { it.amount == 100.0 && it.type == "CREDIT" }) }
    }

    @Test
    fun `GPay debit SMS produces DEBIT TransactionDetected`() = runTest {
        val body = "Rs. 200 debited from your account to merchant@oksbi via GPay. Ref: TEST010"
        val result = useCase.execute("GPAY", body, receivedAt) as ParseResult.Success

        assertEquals(PaymentSource.GPAY, result.parserSource)
        coVerify { transactionDao.insert(match { it.type == "DEBIT" }) }
    }

    @Test
    fun `PhonePe credited-with SMS produces TransactionDetected`() = runTest {
        val body = "Your PhonePe A/c XXXX is credited with Rs 200 by user@ybl. UPI Ref: TEST020"
        val result = useCase.execute("PhonePe", body, receivedAt)

        assertTrue(result is ParseResult.Success)
        assertEquals(PaymentSource.PHONEPE, (result as ParseResult.Success).parserSource)
    }

    @Test
    fun `Paytm credit SMS produces TransactionDetected`() = runTest {
        val body = "Received Rs. 150 from Paytm user. Txn ID: PAYTM100001"
        val result = useCase.execute("PAYTM", body, receivedAt)

        assertTrue(result is ParseResult.Success)
        assertEquals(PaymentSource.PAYTM, (result as ParseResult.Success).parserSource)
        coVerify { transactionDao.insert(match { it.amount == 150.0 }) }
    }

    @Test
    fun `SBI credit SMS produces TransactionDetected`() = runTest {
        val body = "Your A/c XXXX1234 is credited by Rs 500 on 01/01/25. Balance Rs 10500"
        val result = useCase.execute("SBI", body, receivedAt)

        assertTrue(result is ParseResult.Success)
        assertEquals(PaymentSource.SBI, (result as ParseResult.Success).parserSource)
    }

    @Test
    fun `HDFC credit SMS produces TransactionDetected`() = runTest {
        val body = "Rs 300 credited to HDFC Bank A/c XX1234 by NEFT/UPI on 01-01-25."
        val result = useCase.execute("HDFCBK", body, receivedAt)

        assertTrue(result is ParseResult.Success)
        assertEquals(PaymentSource.HDFC, (result as ParseResult.Success).parserSource)
    }

    @Test
    fun `ICICI credit SMS produces TransactionDetected`() = runTest {
        val body = "ICICI Bank: Rs 450.00 credited to A/c XX1234 on 01-Jan-25 by UPI."
        val result = useCase.execute("ICICIB", body, receivedAt)

        assertTrue(result is ParseResult.Success)
        assertEquals(PaymentSource.ICICI, (result as ParseResult.Success).parserSource)
    }

    @Test
    fun `Axis credit SMS produces TransactionDetected`() = runTest {
        val body = "Axis Bank: Rs.600.00 credited to your A/c XXXX on 01Jan25 by UPI."
        val result = useCase.execute("AXISBK", body, receivedAt)

        assertTrue(result is ParseResult.Success)
        assertEquals(PaymentSource.AXIS, (result as ParseResult.Success).parserSource)
    }

    // ── Step 1: Failed transaction pre-check ──────────────────────────────────

    @Test
    fun `GPay failed SMS fires TransactionFailed event before parser is called`() = runTest {
        val body = "Your GPay transaction of Rs. 500 to merchant@okaxis has failed. Please try again."
        val result = useCase.execute("GPAY", body, receivedAt)

        assertTrue(result is ParseResult.Failed)
        assertEquals("transaction_failed_in_sms", (result as ParseResult.Failed).reason)
        coVerify { eventRepository.appendEvent(match { it.eventType == "TransactionFailed" }) }
        // TransactionDetected must NOT be appended
        coVerify(exactly = 0) { eventRepository.appendEvent(match { it.eventType == "TransactionDetected" }) }
    }

    @Test
    fun `failed transaction SMS never reaches parser — no TransactionDetected`() = runTest {
        val body = "Payment of Rs 100 failed. Transaction declined."
        useCase.execute("GPAY", body, receivedAt)

        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }

    // ── Step 2: ParseFailed ───────────────────────────────────────────────────

    @Test
    fun `non-financial SMS produces ParseFailed event`() = runTest {
        val body = "Your OTP is 123456. Do not share with anyone."
        val result = useCase.execute("GPAY", body, receivedAt)

        assertTrue(result is ParseResult.Failed)
        assertEquals("no_parser_matched", (result as ParseResult.Failed).reason)
        coVerify { eventRepository.appendEvent(match { it.eventType == "ParseFailed" }) }
    }

    @Test
    fun `ParseFailed event payload contains masked sender not raw`() = runTest {
        val body = "Your OTP is 123456. Do not share."
        useCase.execute("GPAY", body, receivedAt)

        coVerify {
            eventRepository.appendEvent(match { event: EventEntity ->
                event.eventType == "ParseFailed" &&
                    event.payload.contains("GPAY***") &&
                    !event.payload.contains("\"GPAY\"")
            })
        }
    }

    // ── Step 3: Deduplication ─────────────────────────────────────────────────

    @Test
    fun `identical transaction within 5-minute window is deduplicated`() = runTest {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay. Ref: TEST001"

        // First call succeeds
        val first = useCase.execute("GPAY", body, receivedAt)
        assertTrue(first is ParseResult.Success)

        // Simulate the event store returning the first event in the dedup window
        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(
            EventEntity(
                eventId = "evt-1",
                eventType = "TransactionDetected",
                timestamp = receivedAt,
                payload = """{"amount":100.0,"type":"CREDIT","source":"GPAY","upi_handle_hash":"${com.viis.rozkamai.util.HashUtils.sha256("user@okaxis")}","timestamp":$receivedAt}""",
                version = 1,
            ),
        )

        // Second identical call within window → Duplicate
        val second = useCase.execute("GPAY", body, receivedAt + 1000)
        assertTrue(second is ParseResult.Duplicate)
        coVerify { eventRepository.appendEvent(match { it.eventType == "DuplicateDetected" }) }
    }

    @Test
    fun `different amount is not deduplicated`() = runTest {
        val body1 = "Rs. 100 credited to your account by user@okaxis via GPay."
        val body2 = "Rs. 200 credited to your account by user@okaxis via GPay."

        useCase.execute("GPAY", body1, receivedAt)

        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(
            EventEntity(
                eventId = "evt-1",
                eventType = "TransactionDetected",
                timestamp = receivedAt,
                payload = """{"amount":100.0,"type":"CREDIT","source":"GPAY","timestamp":$receivedAt}""",
                version = 1,
            ),
        )

        val result = useCase.execute("GPAY", body2, receivedAt + 500)
        assertTrue(result is ParseResult.Success)
    }

    // ── Step 4: TransactionDetected payload correctness ───────────────────────

    @Test
    fun `TransactionDetected payload includes amount, type, and source`() = runTest {
        val body = "Rs. 250 credited to your account by payer@okaxis via GPay."
        useCase.execute("GPAY", body, receivedAt)

        coVerify {
            eventRepository.appendEvent(match { event: EventEntity ->
                event.eventType == "TransactionDetected" &&
                    event.payload.contains("250.0") &&
                    event.payload.contains("CREDIT") &&
                    event.payload.contains("GPAY")
            })
        }
    }

    @Test
    fun `TransactionDetected payload never contains raw UPI handle`() = runTest {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay."
        useCase.execute("GPAY", body, receivedAt)

        coVerify {
            eventRepository.appendEvent(match { event: EventEntity ->
                event.eventType == "TransactionDetected" &&
                    !event.payload.contains("user@okaxis")
            })
        }
    }

    // ── TransactionProjector is called on success ─────────────────────────────

    @Test
    fun `successful parse inserts row into transactions table`() = runTest {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay."
        useCase.execute("GPAY", body, receivedAt)

        coVerify { transactionDao.insert(any()) }
    }

    @Test
    fun `failed transaction does not insert into transactions table`() = runTest {
        val body = "Your GPay transaction of Rs. 100 has failed."
        useCase.execute("GPAY", body, receivedAt)

        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }

    @Test
    fun `duplicate does not insert into transactions table`() = runTest {
        val body = "Rs. 100 credited to your account by user@okaxis via GPay."

        coEvery { eventRepository.getTransactionDetectedInWindow(any(), any()) } returns listOf(
            EventEntity(
                eventId = "evt-1",
                eventType = "TransactionDetected",
                timestamp = receivedAt,
                payload = """{"amount":100.0,"type":"CREDIT","source":"GPAY","upi_handle_hash":"${com.viis.rozkamai.util.HashUtils.sha256("user@okaxis")}","timestamp":$receivedAt}""",
                version = 1,
            ),
        )

        useCase.execute("GPAY", body, receivedAt + 60_000)
        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }
}
