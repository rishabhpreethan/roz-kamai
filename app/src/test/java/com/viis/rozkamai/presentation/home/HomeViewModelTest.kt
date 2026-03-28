package com.viis.rozkamai.presentation.home

import app.cash.turbine.test
import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.dao.HourlyStatsDao
import com.viis.rozkamai.data.local.entity.DailySummaryEntity
import com.viis.rozkamai.data.local.entity.HourlyStatsEntity
import com.viis.rozkamai.domain.usecase.InsightCalculator
import com.viis.rozkamai.domain.usecase.WeeklyTrend
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest : BaseUnitTest() {

    private lateinit var dailySummaryDao: DailySummaryDao
    private lateinit var hourlyStatsDao: HourlyStatsDao
    private lateinit var insightCalculator: InsightCalculator
    private lateinit var viewModel: HomeViewModel

    private val summaryFlow = MutableStateFlow<DailySummaryEntity?>(null)
    private val hourlyFlow = MutableStateFlow<List<HourlyStatsEntity>>(emptyList())

    @Before
    override fun setUp() {
        super.setUp()
        dailySummaryDao = mockk()
        hourlyStatsDao = mockk()
        insightCalculator = mockk(relaxed = true)

        every { dailySummaryDao.observeSummaryForDate(any()) } returns summaryFlow
        every { hourlyStatsDao.observeHourlyStats(any()) } returns hourlyFlow
        coEvery { insightCalculator.computeDayOverDayIncome(any()) } returns null
        coEvery { insightCalculator.computeWeeklyTrend(any()) } returns null

        viewModel = HomeViewModel(dailySummaryDao, hourlyStatsDao, insightCalculator)
    }

    // ─── Loading state ────────────────────────────────────────────────────────

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel.uiState.test {
            assertEquals(DashboardUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Empty state (P2-025, P2-031) ────────────────────────────────────────

    @Test
    fun `emits Empty when no summary exists`() = runTest {
        viewModel.uiState.test {
            skipItems(1) // Loading
            assertEquals(DashboardUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when summary has zero transactions (first day edge case)`() = runTest {
        summaryFlow.value = makeSummary(transactionCount = 0, totalIncome = 0.0)
        viewModel.uiState.test {
            skipItems(1) // Loading
            assertEquals(DashboardUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Empty when single transaction arrives then day resets`() = runTest {
        summaryFlow.value = makeSummary(transactionCount = 1, totalIncome = 500.0)
        viewModel.uiState.test {
            skipItems(1) // Loading
            assertTrue(awaitItem() is DashboardUiState.Data)
            summaryFlow.value = null // simulate day reset
            assertEquals(DashboardUiState.Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Data state (P2-030) ─────────────────────────────────────────────────

    @Test
    fun `emits Data with correct totalIncome`() = runTest {
        summaryFlow.value = makeSummary(totalIncome = 5000.0, transactionCount = 10)
        viewModel.uiState.test {
            skipItems(1) // Loading
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(5000.0, data.totalIncome, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Data with correct transactionCount`() = runTest {
        summaryFlow.value = makeSummary(transactionCount = 7)
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(7, data.transactionCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Data with correct avgSaleValue`() = runTest {
        summaryFlow.value = makeSummary(avgTransactionValue = 350.0)
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(350.0, data.avgSaleValue, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Data with expectedIncome from summary entity`() = runTest {
        summaryFlow.value = makeSummary(expectedIncome = 4500.0)
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(4500.0, data.expectedIncome!!, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Data with payment split from summary`() = runTest {
        summaryFlow.value = makeSummary(upiAmount = 3000.0, bankAmount = 1500.0)
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(3000.0, data.upiAmount, 0.001)
            assertEquals(1500.0, data.bankAmount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Data with customer counts from summary`() = runTest {
        summaryFlow.value = makeSummary(newCustomers = 3, returningCustomers = 5)
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(3, data.newCustomers)
            assertEquals(5, data.returningCustomers)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── InsightCalculator delegation ────────────────────────────────────────

    @Test
    fun `dayOverDayIncome comes from InsightCalculator`() = runTest {
        summaryFlow.value = makeSummary()
        coEvery { insightCalculator.computeDayOverDayIncome(any()) } returns 3200.0
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(3200.0, data.dayOverDayIncome!!, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dayOverDayIncome is null when InsightCalculator returns null`() = runTest {
        summaryFlow.value = makeSummary()
        coEvery { insightCalculator.computeDayOverDayIncome(any()) } returns null
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertNull(data.dayOverDayIncome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weeklyTrend comes from InsightCalculator`() = runTest {
        summaryFlow.value = makeSummary()
        val trend = WeeklyTrend(thisWeekIncome = 35000.0, lastWeekIncome = 28000.0, changePercent = 25.0)
        coEvery { insightCalculator.computeWeeklyTrend(any()) } returns trend
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(35000.0, data.weeklyTrend!!.thisWeekIncome, 0.001)
            assertEquals(25.0, data.weeklyTrend.changePercent!!, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Hourly stats mapping ─────────────────────────────────────────────────

    @Test
    fun `hourlyStats filters out zero-transaction hours`() = runTest {
        summaryFlow.value = makeSummary()
        hourlyFlow.value = listOf(
            HourlyStatsEntity(date = viewModel.today, hourBlock = 9, txnCount = 0, totalAmount = 0.0),
            HourlyStatsEntity(date = viewModel.today, hourBlock = 10, txnCount = 3, totalAmount = 600.0),
            HourlyStatsEntity(date = viewModel.today, hourBlock = 11, txnCount = 2, totalAmount = 400.0),
        )
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(2, data.hourlyStats.size)
            assertEquals(10, data.hourlyStats[0].hour)
            assertEquals(11, data.hourlyStats[1].hour)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hourlyStats maps amounts correctly`() = runTest {
        summaryFlow.value = makeSummary()
        hourlyFlow.value = listOf(
            HourlyStatsEntity(date = viewModel.today, hourBlock = 14, txnCount = 4, totalAmount = 800.0),
        )
        viewModel.uiState.test {
            skipItems(1)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(800.0, data.hourlyStats[0].amount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Reactivity ───────────────────────────────────────────────────────────

    @Test
    fun `state updates when summary flow emits new value`() = runTest {
        viewModel.uiState.test {
            skipItems(1) // Loading
            assertEquals(DashboardUiState.Empty, awaitItem())

            summaryFlow.value = makeSummary(totalIncome = 1000.0, transactionCount = 2)
            val data = awaitItem() as DashboardUiState.Data
            assertEquals(1000.0, data.totalIncome, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun makeSummary(
        totalIncome: Double = 1000.0,
        transactionCount: Int = 1,
        avgTransactionValue: Double = 1000.0,
        expectedIncome: Double? = null,
        upiAmount: Double = 1000.0,
        bankAmount: Double = 0.0,
        newCustomers: Int = 0,
        returningCustomers: Int = 0,
    ) = DailySummaryEntity(
        date = viewModel.today,
        totalIncome = totalIncome,
        transactionCount = transactionCount,
        avgTransactionValue = avgTransactionValue,
        expectedIncome = expectedIncome,
        upiAmount = upiAmount,
        bankAmount = bankAmount,
        newCustomers = newCustomers,
        returningCustomers = returningCustomers,
    )
}
