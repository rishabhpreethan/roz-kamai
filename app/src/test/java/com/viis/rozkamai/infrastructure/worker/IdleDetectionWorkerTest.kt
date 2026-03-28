package com.viis.rozkamai.infrastructure.worker

import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Tests for IdleDetectionWorker logic extracted into a testable helper.
 * The actual Worker isn't unit-testable without an Android context;
 * these tests cover the core idle-gap logic using a plain Kotlin equivalent.
 */
class IdleDetectionWorkerTest : BaseUnitTest() {

    private lateinit var transactionDao: TransactionDao
    private lateinit var eventRepository: EventRepository
    private lateinit var logic: IdleDetectionLogic

    @Before
    override fun setUp() {
        super.setUp()
        transactionDao = mockk(relaxed = true)
        eventRepository = mockk(relaxed = true)
        logic = IdleDetectionLogic(transactionDao, eventRepository)
    }

    @Test
    fun `no event fired outside business hours`() = runTest {
        // 08:00 — before business hours
        val now = calAt(8, 0)
        logic.checkIdle(now)
        coVerify(exactly = 0) { eventRepository.appendEvent(any()) }
    }

    @Test
    fun `no event fired after business hours`() = runTest {
        // 21:30 — after business hours (BUSINESS_END_HOUR = 21)
        val now = calAt(21, 30)
        logic.checkIdle(now)
        coVerify(exactly = 0) { eventRepository.appendEvent(any()) }
    }

    @Test
    fun `no event fired when gap is below threshold`() = runTest {
        val now = calAt(14, 0)
        // Last transaction 90 minutes ago (< 120 threshold)
        val lastTxn = now - (90 * 60_000L)
        coEvery { transactionDao.getLastTransactionTime(any()) } returns lastTxn

        logic.checkIdle(now)

        coVerify(exactly = 0) { eventRepository.appendEvent(any()) }
    }

    @Test
    fun `IdleDetected event fired when gap exceeds threshold`() = runTest {
        val now = calAt(14, 0)
        // Last transaction 150 minutes ago (> 120 threshold)
        val lastTxn = now - (150 * 60_000L)
        coEvery { transactionDao.getLastTransactionTime(any()) } returns lastTxn

        logic.checkIdle(now)

        coVerify { eventRepository.appendEvent(any()) }
    }

    @Test
    fun `IdleDetected event has correct eventType`() = runTest {
        val now = calAt(14, 0)
        val lastTxn = now - (150 * 60_000L)
        coEvery { transactionDao.getLastTransactionTime(any()) } returns lastTxn
        val slot = slot<EventEntity>()
        coEvery { eventRepository.appendEvent(capture(slot)) } returns Unit

        logic.checkIdle(now)

        assertEquals("IdleDetected", slot.captured.eventType)
    }

    @Test
    fun `gap is measured from business start when no transactions today`() = runTest {
        // 13:00, no transactions today → gap = 13:00 - 09:00 = 240 min > 120 threshold
        val now = calAt(13, 0)
        coEvery { transactionDao.getLastTransactionTime(any()) } returns null
        val slot = slot<EventEntity>()
        coEvery { eventRepository.appendEvent(capture(slot)) } returns Unit

        logic.checkIdle(now)

        coVerify { eventRepository.appendEvent(any()) }
        assertTrue(slot.captured.payload.contains("gap_minutes"))
    }

    @Test
    fun `no event when no transactions but gap below threshold during morning`() = runTest {
        // 09:45, no transactions → gap = 45 min from business start (09:00) < 120 threshold
        val now = calAt(9, 45)
        coEvery { transactionDao.getLastTransactionTime(any()) } returns null

        logic.checkIdle(now)

        coVerify(exactly = 0) { eventRepository.appendEvent(any()) }
    }

    private fun calAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

/**
 * Extracted logic from IdleDetectionWorker for unit testing without Android context.
 * Mirrors the doWork() logic exactly.
 */
class IdleDetectionLogic(
    private val transactionDao: TransactionDao,
    private val eventRepository: EventRepository,
) {
    suspend fun checkIdle(now: Long) {
        val cal = Calendar.getInstance().also { it.timeInMillis = now }
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)

        if (hourOfDay < IdleDetectionWorker.BUSINESS_START_HOUR ||
            hourOfDay >= IdleDetectionWorker.BUSINESS_END_HOUR
        ) return

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(now))
        val lastTxnTime = transactionDao.getLastTransactionTime(today)

        val gapMinutes: Long = if (lastTxnTime != null) {
            (now - lastTxnTime) / 60_000L
        } else {
            val businessStartMs = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, IdleDetectionWorker.BUSINESS_START_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            (now - businessStartMs) / 60_000L
        }

        if (gapMinutes >= IdleDetectionWorker.IDLE_THRESHOLD_MINUTES) {
            eventRepository.appendEvent(
                EventEntity(
                    eventId = java.util.UUID.randomUUID().toString(),
                    eventType = "IdleDetected",
                    timestamp = now,
                    payload = """{"gap_minutes":$gapMinutes,"last_txn_time":${lastTxnTime ?: 0L},"threshold":${IdleDetectionWorker.IDLE_THRESHOLD_MINUTES}}""",
                    version = 1,
                ),
            )
        }
    }
}
