package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.dao.HourlyStatsDao
import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.local.entity.HourlyStatsEntity
import com.viis.rozkamai.data.local.entity.TransactionEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AggregationEngineTest : BaseUnitTest() {

    private lateinit var transactionDao: TransactionDao
    private lateinit var dailySummaryDao: DailySummaryDao
    private lateinit var hourlyStatsDao: HourlyStatsDao
    private lateinit var insightCalculator: InsightCalculator
    private lateinit var eventRepository: EventRepository
    private lateinit var engine: AggregationEngine

    // 2024-06-15 12:00:00 UTC — a Saturday at noon UTC
    private val testTimestamp = 1718445600000L
    private val testDate = "2024-06-15"

    @Before
    override fun setUp() {
        super.setUp()
        transactionDao = mockk(relaxed = true)
        dailySummaryDao = mockk(relaxed = true)
        hourlyStatsDao = mockk(relaxed = true)
        insightCalculator = mockk(relaxed = true)
        eventRepository = mockk(relaxed = true)

        coEvery { insightCalculator.computeExpectedIncome(any()) } returns null
        coEvery { insightCalculator.computeRunRate(any(), any()) } returns null
        coEvery { insightCalculator.computeConsistencyScore(any()) } returns null
        coEvery { hourlyStatsDao.getPeakHourForDate(any()) } returns null
        coEvery { hourlyStatsDao.getByDateAndHour(any(), any()) } returns null

        engine = AggregationEngine(
            transactionDao, dailySummaryDao, hourlyStatsDao, insightCalculator, eventRepository,
        )
    }

    // ─── Daily totals ─────────────────────────────────────────────────────────

    @Test
    fun `aggregate computes total income from CREDIT transactions only`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns listOf(
            makeTxnEntity(amount = 300.0, type = "CREDIT"),
            makeTxnEntity(amount = 200.0, type = "CREDIT"),
            makeTxnEntity(amount = 500.0, type = "DEBIT"),
        )
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals(500.0, slot.captured.totalIncome, 0.001)
    }

    @Test
    fun `aggregate computes transaction count from CREDIT only`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns listOf(
            makeTxnEntity(amount = 200.0, type = "CREDIT"),
            makeTxnEntity(amount = 300.0, type = "DEBIT"),
        )
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals(1, slot.captured.transactionCount)
    }

    @Test
    fun `aggregate computes average transaction value`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns listOf(
            makeTxnEntity(amount = 100.0, type = "CREDIT"),
            makeTxnEntity(amount = 300.0, type = "CREDIT"),
        )
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals(200.0, slot.captured.avgTransactionValue, 0.001)
    }

    @Test
    fun `aggregate sets avg to zero when no credit transactions`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns emptyList()
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(type = TransactionType.DEBIT), testDate)

        assertEquals(0.0, slot.captured.avgTransactionValue, 0.001)
    }

    // ─── First/last sale time (P2-015) ────────────────────────────────────────

    @Test
    fun `aggregate captures first and last transaction times`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns listOf(
            makeTxnEntity(amount = 100.0, type = "CREDIT", timestamp = 1000L),
            makeTxnEntity(amount = 200.0, type = "CREDIT", timestamp = 3000L),
            makeTxnEntity(amount = 150.0, type = "CREDIT", timestamp = 2000L),
        )
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals(1000L, slot.captured.firstTxnTime)
        assertEquals(3000L, slot.captured.lastTxnTime)
    }

    @Test
    fun `aggregate sets first and last to null when no credits`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns emptyList()
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(type = TransactionType.DEBIT), testDate)

        assertNull(slot.captured.firstTxnTime)
        assertNull(slot.captured.lastTxnTime)
    }

    // ─── Payment split (P2-018) ───────────────────────────────────────────────

    @Test
    fun `aggregate splits UPI vs bank income correctly`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns listOf(
            makeTxnEntity(amount = 500.0, type = "CREDIT", source = "GPAY"),
            makeTxnEntity(amount = 300.0, type = "CREDIT", source = "PHONEPE"),
            makeTxnEntity(amount = 400.0, type = "CREDIT", source = "HDFC"),
        )
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals(800.0, slot.captured.upiAmount, 0.001)
        assertEquals(400.0, slot.captured.bankAmount, 0.001)
    }

    // ─── Hourly stats (P2-003) ────────────────────────────────────────────────

    @Test
    fun `aggregate updates hourly stats for CREDIT transaction`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns listOf(
            makeTxnEntity(amount = 200.0, type = "CREDIT"),
        )

        engine.aggregate(makeParsedTx(type = TransactionType.CREDIT), testDate)

        coVerify { hourlyStatsDao.upsert(any()) }
    }

    @Test
    fun `aggregate does not update hourly stats for DEBIT transaction`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns emptyList()

        engine.aggregate(makeParsedTx(type = TransactionType.DEBIT), testDate)

        coVerify(exactly = 0) { hourlyStatsDao.upsert(any()) }
    }

    @Test
    fun `aggregate increments existing hourly stats`() = runTest {
        coEvery { hourlyStatsDao.getByDateAndHour(testDate, any()) } returns
            HourlyStatsEntity(date = testDate, hourBlock = 12, txnCount = 2, totalAmount = 400.0)
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns listOf(
            makeTxnEntity(amount = 200.0, type = "CREDIT"),
        )
        val slot = slot<HourlyStatsEntity>()
        coEvery { hourlyStatsDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(amount = 100.0), testDate)

        assertEquals(3, slot.captured.txnCount)
        assertEquals(500.0, slot.captured.totalAmount, 0.001)
    }

    // ─── Insight delegation ───────────────────────────────────────────────────

    @Test
    fun `aggregate includes expectedIncome from InsightCalculator`() = runTest {
        coEvery { insightCalculator.computeExpectedIncome(testDate) } returns 2000.0
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns emptyList()
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals(2000.0, slot.captured.expectedIncome!!, 0.001)
    }

    @Test
    fun `aggregate includes consistencyScore from InsightCalculator`() = runTest {
        coEvery { insightCalculator.computeConsistencyScore(testDate) } returns 0.71
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns emptyList()
        val slot = slot<DailySummaryEntity>()
        coEvery { dailySummaryDao.upsert(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals(0.71, slot.captured.consistencyScore!!, 0.001)
    }

    // ─── Event production ─────────────────────────────────────────────────────

    @Test
    fun `aggregate appends DailySummaryComputed event`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns emptyList()
        val slot = slot<EventEntity>()
        coEvery { eventRepository.appendEvent(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assertEquals("DailySummaryComputed", slot.captured.eventType)
        assertNotNull(slot.captured.eventId)
    }

    @Test
    fun `aggregate event payload contains date`() = runTest {
        coEvery { transactionDao.getTransactionsByDate(testDate) } returns emptyList()
        val slot = slot<EventEntity>()
        coEvery { eventRepository.appendEvent(capture(slot)) } returns Unit

        engine.aggregate(makeParsedTx(), testDate)

        assert(slot.captured.payload.contains(testDate))
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun makeParsedTx(
        amount: Double = 200.0,
        type: TransactionType = TransactionType.CREDIT,
        source: PaymentSource = PaymentSource.GPAY,
    ) = ParsedTransaction(
        amount = amount,
        type = type,
        source = source,
        upiHandleHash = null,
        merchantNameHash = null,
        referenceId = null,
        timestamp = testTimestamp,
        rawSenderMasked = "GPAY***",
    )

    private fun makeTxnEntity(
        amount: Double,
        type: String,
        source: String = "GPAY",
        timestamp: Long = testTimestamp,
    ) = TransactionEntity(
        id = "id-${amount.toLong()}",
        eventId = "evt-${amount.toLong()}",
        amount = amount,
        type = type,
        source = source,
        timestamp = timestamp,
        dateBucket = testDate,
        rawSenderMasked = "GPAY***",
        upiHandleHash = null,
        referenceId = null,
        status = "SUCCESS",
    )
}
