package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.CustomerProfileDao
import com.viis.rozkamai.data.local.entity.CustomerProfileEntity
import com.viis.rozkamai.data.local.entity.EventEntity
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerIdentificationServiceTest : BaseUnitTest() {

    private lateinit var customerProfileDao: CustomerProfileDao
    private lateinit var eventRepository: EventRepository
    private lateinit var service: CustomerIdentificationService

    // 2024-06-15 12:00:00 UTC
    private val testTimestamp = 1718445600000L
    private val testDate = "2024-06-15"
    private val testHash = "abc123def456"
    private val testEventId = "evt-test-abc"

    @Before
    override fun setUp() {
        super.setUp()
        customerProfileDao = mockk(relaxed = true)
        eventRepository = mockk(relaxed = true)
        service = CustomerIdentificationService(customerProfileDao, eventRepository)
    }

    // ─── P2-010: Customer identification ─────────────────────────────────────

    @Test
    fun `identify creates new profile for first-time customer`() = runTest {
        coEvery { customerProfileDao.getById(testHash) } returns null
        val slot = slot<CustomerProfileEntity>()
        coEvery { customerProfileDao.upsert(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash), testEventId)

        assertEquals(testHash, slot.captured.customerId)
        assertEquals(testHash, slot.captured.upiHandleHash)
        assertEquals(1, slot.captured.transactionCount)
        assertEquals(250.0, slot.captured.totalAmount, 0.001)
    }

    @Test
    fun `identify sets firstSeenDate and lastSeenDate on new customer`() = runTest {
        coEvery { customerProfileDao.getById(testHash) } returns null
        val slot = slot<CustomerProfileEntity>()
        coEvery { customerProfileDao.upsert(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash), testEventId)

        assertEquals(testDate, slot.captured.firstSeenDate)
        assertEquals(testDate, slot.captured.lastSeenDate)
    }

    @Test
    fun `identify never populates displayName`() = runTest {
        coEvery { customerProfileDao.getById(any()) } returns null
        val slot = slot<CustomerProfileEntity>()
        coEvery { customerProfileDao.upsert(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash), testEventId)

        assertNull(slot.captured.displayName)
    }

    @Test
    fun `identify increments transactionCount for returning customer`() = runTest {
        coEvery { customerProfileDao.getById(testHash) } returns makeExistingProfile(txCount = 3, total = 750.0)
        val slot = slot<CustomerProfileEntity>()
        coEvery { customerProfileDao.upsert(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash, amount = 200.0), testEventId)

        assertEquals(4, slot.captured.transactionCount)
        assertEquals(950.0, slot.captured.totalAmount, 0.001)
    }

    @Test
    fun `identify updates lastSeenDate but preserves firstSeenDate for returning customer`() = runTest {
        coEvery { customerProfileDao.getById(testHash) } returns
            makeExistingProfile(firstSeenDate = "2024-05-01", lastSeenDate = "2024-06-10")
        val slot = slot<CustomerProfileEntity>()
        coEvery { customerProfileDao.upsert(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash), testEventId)

        assertEquals("2024-05-01", slot.captured.firstSeenDate) // unchanged
        assertEquals(testDate, slot.captured.lastSeenDate)       // updated
    }

    @Test
    fun `identify skips DEBIT transactions`() = runTest {
        service.identify(makeTx(type = TransactionType.DEBIT), testEventId)

        coVerify(exactly = 0) { customerProfileDao.upsert(any()) }
        coVerify(exactly = 0) { eventRepository.appendEvent(any()) }
    }

    @Test
    fun `identify skips transactions without UPI hash`() = runTest {
        service.identify(makeTx(upiHandleHash = null), testEventId)

        coVerify(exactly = 0) { customerProfileDao.upsert(any()) }
        coVerify(exactly = 0) { eventRepository.appendEvent(any()) }
    }

    // ─── Event production ─────────────────────────────────────────────────────

    @Test
    fun `identify emits CustomerIdentified event for new customer`() = runTest {
        coEvery { customerProfileDao.getById(any()) } returns null
        val slot = slot<EventEntity>()
        coEvery { eventRepository.appendEvent(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash), testEventId)

        assertEquals("CustomerIdentified", slot.captured.eventType)
        assertTrue(slot.captured.payload.contains(""""is_new":true"""))
    }

    @Test
    fun `identify emits CustomerIdentified event for returning customer`() = runTest {
        coEvery { customerProfileDao.getById(testHash) } returns makeExistingProfile()
        val slot = slot<EventEntity>()
        coEvery { eventRepository.appendEvent(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash), testEventId)

        assertEquals("CustomerIdentified", slot.captured.eventType)
        assertTrue(slot.captured.payload.contains(""""is_new":false"""))
    }

    @Test
    fun `CustomerIdentified event payload contains transaction event ID`() = runTest {
        coEvery { customerProfileDao.getById(any()) } returns null
        val slot = slot<EventEntity>()
        coEvery { eventRepository.appendEvent(capture(slot)) } returns Unit

        service.identify(makeTx(upiHandleHash = testHash), testEventId)

        assertTrue(slot.captured.payload.contains(testEventId))
    }

    // ─── P2-011: Repeat customer detection ───────────────────────────────────

    @Test
    fun `isRepeatCustomer returns true when transactionCount is 2 or more`() {
        val profile = makeExistingProfile(txCount = 2)
        assertTrue(service.isRepeatCustomer(profile))
    }

    @Test
    fun `isRepeatCustomer returns false for first-time customer`() {
        val profile = makeExistingProfile(txCount = 1)
        assertFalse(service.isRepeatCustomer(profile))
    }

    // ─── P2-012: New vs returning classification ──────────────────────────────

    @Test
    fun `isNewCustomer returns true for transactionCount of 1`() {
        assertTrue(service.isNewCustomer(makeExistingProfile(txCount = 1)))
    }

    @Test
    fun `isNewCustomer returns false when transactionCount exceeds 1`() {
        assertFalse(service.isNewCustomer(makeExistingProfile(txCount = 5)))
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun makeTx(
        amount: Double = 250.0,
        type: TransactionType = TransactionType.CREDIT,
        upiHandleHash: String? = testHash,
    ) = ParsedTransaction(
        amount = amount,
        type = type,
        source = PaymentSource.GPAY,
        upiHandleHash = upiHandleHash,
        merchantNameHash = null,
        referenceId = null,
        timestamp = testTimestamp,
        rawSenderMasked = "GPAY***",
    )

    private fun makeExistingProfile(
        txCount: Int = 3,
        total: Double = 750.0,
        firstSeenDate: String = "2024-05-01",
        lastSeenDate: String = "2024-06-10",
    ) = CustomerProfileEntity(
        customerId = testHash,
        displayName = null,
        upiHandleHash = testHash,
        firstSeen = 1000L,
        lastSeen = 2000L,
        firstSeenDate = firstSeenDate,
        lastSeenDate = lastSeenDate,
        transactionCount = txCount,
        totalAmount = total,
    )
}
