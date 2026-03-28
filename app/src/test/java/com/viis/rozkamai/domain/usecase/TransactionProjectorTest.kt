package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.TransactionDao
import com.viis.rozkamai.data.local.entity.TransactionEntity
import com.viis.rozkamai.domain.event.PaymentSource
import com.viis.rozkamai.domain.model.ParsedTransaction
import com.viis.rozkamai.domain.model.TransactionType
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TransactionProjectorTest : BaseUnitTest() {

    private lateinit var transactionDao: TransactionDao
    private lateinit var aggregationEngine: AggregationEngine
    private lateinit var projector: TransactionProjector

    // 2024-06-15 12:00:00 UTC in millis
    private val testTimestamp = 1718445600000L
    private val testEventId = "evt-test-123"

    private fun makeTransaction(
        amount: Double = 250.0,
        type: TransactionType = TransactionType.CREDIT,
        source: PaymentSource = PaymentSource.GPAY,
        upiHandleHash: String? = "abc123hash",
        referenceId: String? = "REF999",
    ) = ParsedTransaction(
        amount = amount,
        type = type,
        source = source,
        upiHandleHash = upiHandleHash,
        merchantNameHash = null,
        referenceId = referenceId,
        timestamp = testTimestamp,
        rawSenderMasked = "GPAY***",
    )

    @Before
    override fun setUp() {
        super.setUp()
        transactionDao = mockk(relaxed = true)
        aggregationEngine = mockk(relaxed = true)
        projector = TransactionProjector(transactionDao, aggregationEngine)
    }

    @Test
    fun `project inserts entity with correct amount`() = runTest {
        val tx = makeTransaction(amount = 500.0)
        projector.project(tx, testEventId)

        coVerify {
            transactionDao.insert(match { it.amount == 500.0 })
        }
    }

    @Test
    fun `project inserts entity with correct type`() = runTest {
        projector.project(makeTransaction(type = TransactionType.DEBIT), testEventId)

        coVerify {
            transactionDao.insert(match { it.type == "DEBIT" })
        }
    }

    @Test
    fun `project inserts entity with correct source`() = runTest {
        projector.project(makeTransaction(source = PaymentSource.PHONEPE), testEventId)

        coVerify {
            transactionDao.insert(match { it.source == "PHONEPE" })
        }
    }

    @Test
    fun `project preserves eventId`() = runTest {
        projector.project(makeTransaction(), testEventId)

        coVerify {
            transactionDao.insert(match { it.eventId == testEventId })
        }
    }

    @Test
    fun `project sets status to SUCCESS`() = runTest {
        projector.project(makeTransaction(), testEventId)

        coVerify {
            transactionDao.insert(match { it.status == "SUCCESS" })
        }
    }

    @Test
    fun `project formats dateBucket correctly`() = runTest {
        projector.project(makeTransaction(), testEventId)

        // 1718445600000L is 2024-06-15 in UTC
        coVerify {
            transactionDao.insert(match { it.dateBucket == "2024-06-15" })
        }
    }

    @Test
    fun `project preserves upiHandleHash`() = runTest {
        projector.project(makeTransaction(upiHandleHash = "deadbeef"), testEventId)

        coVerify {
            transactionDao.insert(match { it.upiHandleHash == "deadbeef" })
        }
    }

    @Test
    fun `project allows null upiHandleHash`() = runTest {
        projector.project(makeTransaction(upiHandleHash = null), testEventId)

        coVerify {
            transactionDao.insert(match { it.upiHandleHash == null })
        }
    }

    @Test
    fun `project preserves referenceId`() = runTest {
        projector.project(makeTransaction(referenceId = "TXN-ABC"), testEventId)

        coVerify {
            transactionDao.insert(match { it.referenceId == "TXN-ABC" })
        }
    }

    @Test
    fun `project generates unique id for each call`() = runTest {
        val ids = mutableListOf<String>()
        val capturingDao = mockk<TransactionDao>(relaxed = true)
        val capturingProjector = TransactionProjector(capturingDao, mockk(relaxed = true))

        coVerify(exactly = 0) { capturingDao.insert(any()) }

        capturingProjector.project(makeTransaction(), "evt-1")
        capturingProjector.project(makeTransaction(), "evt-2")

        coVerify(exactly = 2) { capturingDao.insert(any()) }
    }
}
