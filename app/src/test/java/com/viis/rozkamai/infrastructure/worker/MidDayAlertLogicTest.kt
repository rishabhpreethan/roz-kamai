package com.viis.rozkamai.infrastructure.worker

import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MidDayAlertLogicTest : BaseUnitTest() {

    private lateinit var dailySummaryDao: DailySummaryDao
    private lateinit var logic: MidDayAlertLogic

    private val testDate = "2024-06-15"

    @Before
    override fun setUp() {
        super.setUp()
        dailySummaryDao = mockk()
        logic = MidDayAlertLogic(dailySummaryDao)
    }

    @Test
    fun `returns null when no summary for date`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns null

        assertNull(logic.computeAlertContent(testDate))
    }

    @Test
    fun `returns null when no expected income baseline`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(totalIncome = 500.0, expectedIncome = null)

        assertNull(logic.computeAlertContent(testDate))
    }

    @Test
    fun `returns null when expected income is zero`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(totalIncome = 0.0, expectedIncome = 0.0)

        assertNull(logic.computeAlertContent(testDate))
    }

    @Test
    fun `returns null when income meets threshold (exactly 70 percent)`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(totalIncome = 700.0, expectedIncome = 1000.0)

        // 700 >= 1000 * 0.70 → no alert
        assertNull(logic.computeAlertContent(testDate))
    }

    @Test
    fun `returns null when income exceeds expected`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(totalIncome = 1200.0, expectedIncome = 1000.0)

        assertNull(logic.computeAlertContent(testDate))
    }

    @Test
    fun `returns content when income is below 70 percent of expected`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(totalIncome = 500.0, expectedIncome = 1000.0)

        // 500 < 1000 * 0.70 → alert
        val content = logic.computeAlertContent(testDate)
        assertNotNull(content)
        assertEquals(500.0, content!!.currentIncome, 0.001)
        assertEquals(1000.0, content.expectedIncome, 0.001)
    }

    @Test
    fun `returns content when income is well below threshold`() = runTest {
        // 100 / 2000 = 5% — far below 70%
        coEvery { dailySummaryDao.getSummaryForDate(testDate) } returns
            makeSummary(totalIncome = 100.0, expectedIncome = 2000.0)

        assertNotNull(logic.computeAlertContent(testDate))
    }

    @Test
    fun `alert threshold is 70 percent`() {
        assertEquals(0.70, MidDayAlertLogic.INCOME_THRESHOLD, 0.001)
    }

    private fun makeSummary(
        totalIncome: Double = 500.0,
        expectedIncome: Double? = 1000.0,
    ) = DailySummaryEntity(
        date = testDate,
        totalIncome = totalIncome,
        expectedIncome = expectedIncome,
    )
}
