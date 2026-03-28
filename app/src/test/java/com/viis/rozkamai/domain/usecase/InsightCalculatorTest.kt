package com.viis.rozkamai.domain.usecase

import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.dao.HourlyStatsDao
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import com.viis.rozkamai.data.local.entity.HourlyStatsEntity
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InsightCalculatorTest : BaseUnitTest() {

    private lateinit var dailySummaryDao: DailySummaryDao
    private lateinit var hourlyStatsDao: HourlyStatsDao
    private lateinit var calculator: InsightCalculator

    // 2024-06-15 is a Saturday → strftime('%w') = 6, Calendar.DAY_OF_WEEK = 7 → weekday = "6"
    private val testDate = "2024-06-15"

    @Before
    override fun setUp() {
        super.setUp()
        dailySummaryDao = mockk()
        hourlyStatsDao = mockk()
        calculator = InsightCalculator(dailySummaryDao, hourlyStatsDao)
    }

    // ─── computeExpectedIncome ────────────────────────────────────────────────

    @Test
    fun `computeExpectedIncome returns null when fewer than 3 historical summaries`() = runTest {
        coEvery { dailySummaryDao.getSummariesForWeekday(any(), any()) } returns listOf(
            makeSummary("2024-06-08", 1000.0),
            makeSummary("2024-06-01", 1200.0),
        )
        assertNull(calculator.computeExpectedIncome(testDate))
    }

    @Test
    fun `computeExpectedIncome averages same-weekday history`() = runTest {
        coEvery { dailySummaryDao.getSummariesForWeekday(any(), any()) } returns listOf(
            makeSummary("2024-06-08", 900.0),
            makeSummary("2024-06-01", 1100.0),
            makeSummary("2024-05-25", 1000.0),
        )
        val result = calculator.computeExpectedIncome(testDate)
        assertNotNull(result)
        assertEquals(1000.0, result!!, 0.001)
    }

    @Test
    fun `computeExpectedIncome excludes today from history`() = runTest {
        coEvery { dailySummaryDao.getSummariesForWeekday(any(), any()) } returns listOf(
            makeSummary(testDate, 5000.0), // today — should be excluded
            makeSummary("2024-06-08", 1000.0),
            makeSummary("2024-06-01", 1000.0),
            makeSummary("2024-05-25", 1000.0),
        )
        val result = calculator.computeExpectedIncome(testDate)
        // Only the 3 historical entries remain → avg = 1000
        assertEquals(1000.0, result!!, 0.001)
    }

    @Test
    fun `computeExpectedIncome returns null for invalid date`() = runTest {
        val result = calculator.computeExpectedIncome("not-a-date")
        assertNull(result)
    }

    // ─── computeRunRate ───────────────────────────────────────────────────────

    @Test
    fun `computeRunRate returns null before 09h30`() {
        // 09:15 on any day
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 15)
        }
        assertNull(calculator.computeRunRate(500.0, cal.timeInMillis))
    }

    @Test
    fun `computeRunRate returns null when income is zero`() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 0)
        }
        assertNull(calculator.computeRunRate(0.0, cal.timeInMillis))
    }

    @Test
    fun `computeRunRate projects to end of business day`() {
        // At 13:00, halfway through 09:00–22:00 = 780 min total, 240 min elapsed
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 13)
            set(java.util.Calendar.MINUTE, 0)
        }
        val income = 240.0 // 1.0 per minute so far
        val result = calculator.computeRunRate(income, cal.timeInMillis)
        assertNotNull(result)
        // rate = 240/240 = 1 per min, projected = 1 * 780 = 780
        assertEquals(780.0, result!!, 1.0) // allow rounding
    }

    // ─── computeSlowHours ─────────────────────────────────────────────────────

    @Test
    fun `computeSlowHours returns empty list when fewer than 3 active hours`() = runTest {
        coEvery { hourlyStatsDao.getHourlyStatsForDate(testDate) } returns listOf(
            makeHourlyStats(10, 500.0),
            makeHourlyStats(11, 300.0),
        )
        assertTrue(calculator.computeSlowHours(testDate).isEmpty())
    }

    @Test
    fun `computeSlowHours returns hours below 50 percent of average`() = runTest {
        // avg = (400+500+600+50)/4 = 387.5 → threshold = 193.75 → hour 13 (50) is slow
        coEvery { hourlyStatsDao.getHourlyStatsForDate(testDate) } returns listOf(
            makeHourlyStats(10, 400.0),
            makeHourlyStats(11, 500.0),
            makeHourlyStats(12, 600.0),
            makeHourlyStats(13, 50.0),
        )
        val slow = calculator.computeSlowHours(testDate)
        assertEquals(listOf(13), slow)
    }

    @Test
    fun `computeSlowHours skips hours with zero transactions`() = runTest {
        // Hours with txnCount=0 are excluded from active list
        coEvery { hourlyStatsDao.getHourlyStatsForDate(testDate) } returns listOf(
            makeHourlyStats(8, 0.0, txnCount = 0),
            makeHourlyStats(10, 500.0),
            makeHourlyStats(11, 600.0),
            makeHourlyStats(12, 550.0),
        )
        // 3 active hours, avg = (500+600+550)/3 = 550, threshold = 275 → none slow
        assertTrue(calculator.computeSlowHours(testDate).isEmpty())
    }

    // ─── computeDayOverDayIncome ──────────────────────────────────────────────

    @Test
    fun `computeDayOverDayIncome returns yesterday income`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate("2024-06-14") } returns
            makeSummary("2024-06-14", 3500.0)
        assertEquals(3500.0, calculator.computeDayOverDayIncome(testDate)!!, 0.001)
    }

    @Test
    fun `computeDayOverDayIncome returns null when no record for yesterday`() = runTest {
        coEvery { dailySummaryDao.getSummaryForDate("2024-06-14") } returns null
        assertNull(calculator.computeDayOverDayIncome(testDate))
    }

    // ─── computeConsistencyScore ──────────────────────────────────────────────

    @Test
    fun `computeConsistencyScore returns null when fewer than 7 historical days`() = runTest {
        coEvery { dailySummaryDao.getRecentSummaries(any()) } returns listOf(
            makeSummary("2024-06-14", 1000.0),
            makeSummary("2024-06-13", 0.0),
        )
        assertNull(calculator.computeConsistencyScore(testDate))
    }

    @Test
    fun `computeConsistencyScore computes fraction of days with income`() = runTest {
        // 5 of 7 days have income > 0
        coEvery { dailySummaryDao.getRecentSummaries(any()) } returns listOf(
            makeSummary(testDate, 1000.0),       // today — excluded
            makeSummary("2024-06-14", 1000.0),
            makeSummary("2024-06-13", 0.0),       // no income
            makeSummary("2024-06-12", 800.0),
            makeSummary("2024-06-11", 1200.0),
            makeSummary("2024-06-10", 0.0),       // no income
            makeSummary("2024-06-09", 900.0),
            makeSummary("2024-06-08", 1100.0),
        )
        val score = calculator.computeConsistencyScore(testDate)
        assertNotNull(score)
        assertEquals(5.0 / 7.0, score!!, 0.001)
    }

    @Test
    fun `computeConsistencyScore is 1 when all 7 days have income`() = runTest {
        coEvery { dailySummaryDao.getRecentSummaries(any()) } returns listOf(
            makeSummary("2024-06-14", 1000.0),
            makeSummary("2024-06-13", 800.0),
            makeSummary("2024-06-12", 900.0),
            makeSummary("2024-06-11", 1100.0),
            makeSummary("2024-06-10", 700.0),
            makeSummary("2024-06-09", 600.0),
            makeSummary("2024-06-08", 500.0),
        )
        assertEquals(1.0, calculator.computeConsistencyScore(testDate)!!, 0.001)
    }

    // ─── getWeekday ───────────────────────────────────────────────────────────

    @Test
    fun `getWeekday returns correct value for Saturday (2024-06-15)`() {
        // 2024-06-15 is a Saturday → strftime('%w') = 6
        assertEquals("6", calculator.getWeekday("2024-06-15"))
    }

    @Test
    fun `getWeekday returns 0 for Sunday`() {
        // 2024-06-16 is a Sunday
        assertEquals("0", calculator.getWeekday("2024-06-16"))
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun makeSummary(date: String, income: Double) = DailySummaryEntity(
        date = date,
        totalIncome = income,
    )

    private fun makeHourlyStats(hour: Int, amount: Double, txnCount: Int = 1) = HourlyStatsEntity(
        date = testDate,
        hourBlock = hour,
        txnCount = txnCount,
        totalAmount = amount,
    )
}
