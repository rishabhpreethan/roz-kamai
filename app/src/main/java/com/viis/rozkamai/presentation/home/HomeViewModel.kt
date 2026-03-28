package com.viis.rozkamai.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viis.rozkamai.data.local.dao.DailySummaryDao
import com.viis.rozkamai.data.local.dao.HourlyStatsDao
import com.viis.rozkamai.domain.usecase.InsightCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailySummaryDao: DailySummaryDao,
    private val hourlyStatsDao: HourlyStatsDao,
    private val insightCalculator: InsightCalculator,
) : ViewModel() {

    val today: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }.format(Date())

    val uiState: StateFlow<DashboardUiState> = combine(
        dailySummaryDao.observeSummaryForDate(today),
        hourlyStatsDao.observeHourlyStats(today),
    ) { summary, hourlyStatEntities ->
        if (summary == null || summary.transactionCount == 0) {
            DashboardUiState.Empty
        } else {
            val dayOverDay = insightCalculator.computeDayOverDayIncome(today)
            val weeklyTrend = insightCalculator.computeWeeklyTrend(today)
            val hourlyStats = hourlyStatEntities
                .filter { it.txnCount > 0 }
                .map { HourlyStatUi(it.hourBlock, it.totalAmount) }
            DashboardUiState.Data(
                totalIncome = summary.totalIncome,
                transactionCount = summary.transactionCount,
                avgSaleValue = summary.avgTransactionValue,
                firstSaleTime = summary.firstTxnTime,
                lastSaleTime = summary.lastTxnTime,
                dayOverDayIncome = dayOverDay,
                expectedIncome = summary.expectedIncome,
                runRateProjection = summary.runRateProjection,
                consistencyScore = summary.consistencyScore,
                peakHour = summary.peakHour,
                upiAmount = summary.upiAmount,
                bankAmount = summary.bankAmount,
                newCustomers = summary.newCustomers,
                returningCustomers = summary.returningCustomers,
                hourlyStats = hourlyStats,
                weeklyTrend = weeklyTrend,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState.Loading,
    )
}
