package com.viis.rozkamai.presentation.home

import com.viis.rozkamai.domain.usecase.WeeklyTrend

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    object Empty : DashboardUiState()
    data class Data(
        val totalIncome: Double,
        val transactionCount: Int,
        val avgSaleValue: Double,
        val firstSaleTime: Long?,
        val lastSaleTime: Long?,
        val dayOverDayIncome: Double?,
        val expectedIncome: Double?,
        val runRateProjection: Double?,
        val consistencyScore: Double?,
        val peakHour: Int?,
        val upiAmount: Double,
        val bankAmount: Double,
        val newCustomers: Int,
        val returningCustomers: Int,
        val hourlyStats: List<HourlyStatUi>,
        val weeklyTrend: WeeklyTrend?,
    ) : DashboardUiState()
}

data class HourlyStatUi(val hour: Int, val amount: Double)
