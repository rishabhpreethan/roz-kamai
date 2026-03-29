package com.viis.rozkamai.infrastructure.worker

import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import com.viis.rozkamai.domain.usecase.InsightCalculator
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class EodSummaryLogicTest : BaseUnitTest() {

    private lateinit var dailySummaryDao: DailySummaryDao
    private lateinit var insightCalculator: InsightCalculator
    private lateinit var logic: EodSummaryLogic

    private val testDate = "2024-06-15"

    @Before
    override fun setUp() {
        super.setUp()
        dailySummaryDao = mockk()
        insightCalculator = mockk(relaxed = true)
        logic = EodSummaryLogic(dailySummaryDao, insightCalculator)
    }

    @Test
    fun `returns null when no summary for date`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns null

        assertNull(logic.computeContent(testDate))
    }

    @Test
    fun `returns null when summary has zero transactions`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(transactionCount = 0, totalIncome = 0.0)

        assertNull(logic.computeContent(testDate))
    }

    @Test
    fun `returns content with correct totalIncome`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(totalIncome = 4500.0, transactionCount = 9)

        val content = logic.computeContent(testDate)
        assertNotNull(content)
        assertEquals(4500.0, content!!.totalIncome, 0.001)
    }

    @Test
    fun `returns content with correct transactionCount`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(transactionCount = 12)

        assertEquals(12, logic.computeContent(testDate)!!.transactionCount)
    }

    @Test
    fun `dayOverDayIncome comes from InsightCalculator`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns makeSummary()
        coEvery { insightCalculator.computeDayOverDayIncome(testDate) } returns 3200.0

        assertEquals(3200.0, logic.computeContent(testDate)!!.dayOverDayIncome!!, 0.001)
    }

    @Test
    fun `dayOverDayIncome is null when InsightCalculator returns null`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns makeSummary()
        coEvery { insightCalculator.computeDayOverDayIncome(testDate) } returns null

        assertNull(logic.computeContent(testDate)!!.dayOverDayIncome)
    }

    private fun makeSummary(
        totalIncome: Double = 1000.0,
        transactionCount: Int = 3,
    ) = DailySummaryEntity(
        date = testDate,
        totalIncome = totalIncome,
        transactionCount = transactionCount,
    )
}
