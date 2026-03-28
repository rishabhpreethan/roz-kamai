package com.viis.rozkamai.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viis.rozkamai.R
import com.viis.rozkamai.domain.usecase.WeeklyTrend
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        when (val state = uiState) {
            DashboardUiState.Loading -> LoadingContent(Modifier.padding(padding))
            DashboardUiState.Empty -> EmptyContent(Modifier.padding(padding))
            is DashboardUiState.Data -> DataContent(state, Modifier.padding(padding))
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

// ─── Empty state (P2-025) ─────────────────────────────────────────────────────

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "₹",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.dashboard_no_transactions),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Data state ───────────────────────────────────────────────────────────────

@Composable
private fun DataContent(state: DashboardUiState.Data, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // P2-020: Main income card
        item {
            IncomeCard(
                totalIncome = state.totalIncome,
                transactionCount = state.transactionCount,
                dayOverDayIncome = state.dayOverDayIncome,
            )
        }

        // P2-024: Expected vs actual + run rate
        if (state.expectedIncome != null || state.runRateProjection != null) {
            item {
                InsightsRow(
                    expectedIncome = state.expectedIncome,
                    actualIncome = state.totalIncome,
                    runRateProjection = state.runRateProjection,
                )
            }
        }

        // P2-021: Hourly breakdown
        if (state.hourlyStats.isNotEmpty()) {
            item {
                HourlyBreakdownCard(
                    hourlyStats = state.hourlyStats,
                    peakHour = state.peakHour,
                )
            }
        }

        // P2-022: Customer insights + payment split
        item {
            BottomInsightsRow(
                newCustomers = state.newCustomers,
                returningCustomers = state.returningCustomers,
                upiAmount = state.upiAmount,
                bankAmount = state.bankAmount,
                avgSaleValue = state.avgSaleValue,
                consistencyScore = state.consistencyScore,
            )
        }

        // P2-023: Weekly trend
        if (state.weeklyTrend != null) {
            item {
                WeeklyTrendCard(trend = state.weeklyTrend)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ─── Income card (P2-020) ─────────────────────────────────────────────────────

@Composable
private fun IncomeCard(
    totalIncome: Double,
    transactionCount: Int,
    dayOverDayIncome: Double?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = "Aaj ki kamai",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatAmount(totalIncome),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$transactionCount payment${if (transactionCount == 1) "" else "s"} aaye",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            if (dayOverDayIncome != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val diff = totalIncome - dayOverDayIncome
                val comparisonText = when {
                    diff > 0.5 -> stringResource(R.string.dashboard_vs_yesterday_more, formatAmountPlain(abs(diff)))
                    diff < -0.5 -> stringResource(R.string.dashboard_vs_yesterday_less, formatAmountPlain(abs(diff)))
                    else -> stringResource(R.string.dashboard_vs_yesterday_same)
                }
                Text(
                    text = comparisonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

// ─── Expected vs actual + run rate (P2-024) ───────────────────────────────────

@Composable
private fun InsightsRow(
    expectedIncome: Double?,
    actualIncome: Double,
    runRateProjection: Double?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (expectedIncome != null) {
            SmallInfoCard(
                modifier = Modifier.weight(1f),
                label = "Expected tha",
                value = formatAmount(expectedIncome),
                highlight = actualIncome >= expectedIncome,
            )
        }
        if (runRateProjection != null) {
            SmallInfoCard(
                modifier = Modifier.weight(1f),
                label = "Aaj tak ja sakta hai",
                value = formatAmount(runRateProjection),
            )
        }
    }
}

// ─── Hourly breakdown card (P2-021) ───────────────────────────────────────────

@Composable
private fun HourlyBreakdownCard(
    hourlyStats: List<HourlyStatUi>,
    peakHour: Int?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ghante ke hisaab se",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (peakHour != null) {
                    Text(
                        text = "Peak: ${formatHour(peakHour)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HourlyBarChart(stats = hourlyStats)
        }
    }
}

@Composable
private fun HourlyBarChart(stats: List<HourlyStatUi>) {
    val maxAmount = stats.maxOfOrNull { it.amount } ?: 1.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        stats.forEach { stat ->
            val fraction = (stat.amount / maxAmount).toFloat().coerceIn(0.05f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${stat.hour}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Bottom insights row (P2-022) ─────────────────────────────────────────────

@Composable
private fun BottomInsightsRow(
    newCustomers: Int,
    returningCustomers: Int,
    upiAmount: Double,
    bankAmount: Double,
    avgSaleValue: Double,
    consistencyScore: Double?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Customer insights (P2-022)
            if (newCustomers > 0 || returningCustomers > 0) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Grahak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (newCustomers > 0) {
                            Text(
                                text = "$newCustomers naye",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (returningCustomers > 0) {
                            Text(
                                text = "$returningCustomers purane",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Payment split (P2-018)
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Payment split",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (upiAmount > 0) {
                        Text(
                            text = "UPI ${formatAmount(upiAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (bankAmount > 0) {
                        Text(
                            text = "Bank ${formatAmount(bankAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avg sale value (P2-009)
            SmallInfoCard(
                modifier = Modifier.weight(1f),
                label = "Avg sale",
                value = formatAmount(avgSaleValue),
            )

            // Consistency score (P2-019)
            if (consistencyScore != null) {
                val daysWithIncome = (consistencyScore * 7).roundToInt()
                SmallInfoCard(
                    modifier = Modifier.weight(1f),
                    label = "7 mein se",
                    value = "$daysWithIncome din income aaya",
                )
            }
        }
    }
}

// ─── Weekly trend card (P2-023) ───────────────────────────────────────────────

@Composable
private fun WeeklyTrendCard(trend: WeeklyTrend) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly trend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Is hafte",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatAmount(trend.thisWeekIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pichle hafte",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatAmount(trend.lastWeekIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (trend.changePercent != null) {
                    val pct = trend.changePercent
                    val label = if (pct >= 0) "+${pct.roundToInt()}%" else "${pct.roundToInt()}%"
                    val color = if (pct >= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

// ─── Reusable small info card ─────────────────────────────────────────────────

@Composable
private fun SmallInfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatAmount(amount: Double): String = "₹${"%,.0f".format(amount)}"

private fun formatAmountPlain(amount: Double): String = "%,.0f".format(amount)

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 raat"
    hour < 12 -> "$hour subah"
    hour == 12 -> "12 dopahar"
    else -> "${hour - 12} shaam"
}
