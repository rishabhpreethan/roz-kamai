package com.viis.rozkamai.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.viis.rozkamai.domain.usecase.WeeklyTrend
import com.viis.rozkamai.presentation.theme.RozKamaiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI tests for the Dashboard screen (P2-029).
 *
 * Uses [HomeScreenContent] (internal) to inject state directly — no Hilt required.
 * Tests verify text content rendered for each [DashboardUiState] variant and that
 * optional sections (hourly chart, weekly trend) show/hide correctly.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ─── Loading state ────────────────────────────────────────────────────────

    @Test
    fun loadingState_doesNotShowIncomeOrEmptyText() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = DashboardUiState.Loading)
            }
        }
        composeRule.onNodeWithText("Aaj ki kamai").assertDoesNotExist()
        composeRule.onNodeWithText("Jaise hi UPI/bank payment aayega, yahan dikhega")
            .assertDoesNotExist()
    }

    // ─── Empty state (P2-025) ─────────────────────────────────────────────────

    @Test
    fun emptyState_showsNoTransactionsMessage() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = DashboardUiState.Empty)
            }
        }
        composeRule.onNodeWithText("Jaise hi UPI/bank payment aayega, yahan dikhega")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_doesNotShowIncomeCard() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = DashboardUiState.Empty)
            }
        }
        composeRule.onNodeWithText("Aaj ki kamai").assertDoesNotExist()
    }

    // ─── Data state (P2-020) ─────────────────────────────────────────────────

    @Test
    fun dataState_showsAajKiKamaiLabel() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState())
            }
        }
        composeRule.onNodeWithText("Aaj ki kamai").assertIsDisplayed()
    }

    @Test
    fun dataState_showsTransactionCount() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(transactionCount = 5))
            }
        }
        composeRule.onNodeWithText("5 payments aaye").assertIsDisplayed()
    }

    @Test
    fun dataState_singularTransactionCount() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(transactionCount = 1))
            }
        }
        composeRule.onNodeWithText("1 payment aaye").assertIsDisplayed()
    }

    @Test
    fun dataState_showsDayOverDayMore_whenHigherThanYesterday() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(
                    uiState = makeDataState(
                        totalIncome = 2000.0,
                        dayOverDayIncome = 1500.0,
                    ),
                )
            }
        }
        composeRule.onNodeWithText("Kal se", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("zyada", substring = true).assertIsDisplayed()
    }

    @Test
    fun dataState_showsDayOverDayLess_whenLowerThanYesterday() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(
                    uiState = makeDataState(
                        totalIncome = 1000.0,
                        dayOverDayIncome = 1500.0,
                    ),
                )
            }
        }
        composeRule.onNodeWithText("kam", substring = true).assertIsDisplayed()
    }

    @Test
    fun dataState_showsDayOverDaySame_whenEqualToYesterday() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(
                    uiState = makeDataState(
                        totalIncome = 1000.0,
                        dayOverDayIncome = 1000.0,
                    ),
                )
            }
        }
        composeRule.onNodeWithText("Kal jitna hi aaya").assertIsDisplayed()
    }

    // ─── Expected income section (P2-024) ─────────────────────────────────────

    @Test
    fun dataState_showsExpectedIncome_whenPresent() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(expectedIncome = 3000.0))
            }
        }
        composeRule.onNodeWithText("Expected tha").assertIsDisplayed()
    }

    @Test
    fun dataState_hidesExpectedIncome_whenNull() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(expectedIncome = null))
            }
        }
        composeRule.onNodeWithText("Expected tha").assertDoesNotExist()
    }

    @Test
    fun dataState_showsRunRate_whenPresent() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(runRateProjection = 5000.0))
            }
        }
        composeRule.onNodeWithText("Aaj tak ja sakta hai").assertIsDisplayed()
    }

    // ─── Hourly breakdown section (P2-021) ────────────────────────────────────

    @Test
    fun dataState_showsHourlySection_whenStatsPresent() {
        val stats = listOf(
            HourlyStatUi(hour = 10, amount = 500.0),
            HourlyStatUi(hour = 11, amount = 300.0),
        )
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(hourlyStats = stats))
            }
        }
        composeRule.onNodeWithText("Ghante ke hisaab se").assertIsDisplayed()
    }

    @Test
    fun dataState_hidesHourlySection_whenStatsEmpty() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(hourlyStats = emptyList()))
            }
        }
        composeRule.onNodeWithText("Ghante ke hisaab se").assertDoesNotExist()
    }

    // ─── Customer insights section (P2-022) ───────────────────────────────────

    @Test
    fun dataState_showsCustomerSection_whenCustomersPresent() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(newCustomers = 2, returningCustomers = 1))
            }
        }
        composeRule.onNodeWithText("Grahak").assertIsDisplayed()
        composeRule.onNodeWithText("2 naye").assertIsDisplayed()
        composeRule.onNodeWithText("1 purane").assertIsDisplayed()
    }

    // ─── Weekly trend section (P2-023) ────────────────────────────────────────

    @Test
    fun dataState_showsWeeklyTrend_whenPresent() {
        val trend = WeeklyTrend(
            thisWeekIncome = 42000.0,
            lastWeekIncome = 35000.0,
            changePercent = 20.0,
        )
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(weeklyTrend = trend))
            }
        }
        composeRule.onNodeWithText("Weekly trend").assertIsDisplayed()
        composeRule.onNodeWithText("Is hafte").assertIsDisplayed()
        composeRule.onNodeWithText("Pichle hafte").assertIsDisplayed()
        composeRule.onNodeWithText("+20%").assertIsDisplayed()
    }

    @Test
    fun dataState_hidesWeeklyTrend_whenNull() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(weeklyTrend = null))
            }
        }
        composeRule.onNodeWithText("Weekly trend").assertDoesNotExist()
    }

    // ─── Avg sale & consistency (P2-009, P2-019) ──────────────────────────────

    @Test
    fun dataState_showsAvgSaleLabel() {
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState())
            }
        }
        composeRule.onNodeWithText("Avg sale").assertIsDisplayed()
    }

    @Test
    fun dataState_showsConsistencyScore_whenPresent() {
        // 5/7 days → score = 5/7 ≈ 0.714 → "5 din income aaya"
        composeRule.setContent {
            RozKamaiTheme {
                HomeScreenContent(uiState = makeDataState(consistencyScore = 5.0 / 7.0))
            }
        }
        composeRule.onNodeWithText("7 mein se").assertIsDisplayed()
        composeRule.onNodeWithText("5 din income aaya").assertIsDisplayed()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun makeDataState(
        totalIncome: Double = 1000.0,
        transactionCount: Int = 3,
        avgSaleValue: Double = 333.0,
        dayOverDayIncome: Double? = null,
        expectedIncome: Double? = null,
        runRateProjection: Double? = null,
        consistencyScore: Double? = null,
        newCustomers: Int = 0,
        returningCustomers: Int = 0,
        hourlyStats: List<HourlyStatUi> = emptyList(),
        weeklyTrend: WeeklyTrend? = null,
    ) = DashboardUiState.Data(
        totalIncome = totalIncome,
        transactionCount = transactionCount,
        avgSaleValue = avgSaleValue,
        firstSaleTime = null,
        lastSaleTime = null,
        dayOverDayIncome = dayOverDayIncome,
        expectedIncome = expectedIncome,
        runRateProjection = runRateProjection,
        consistencyScore = consistencyScore,
        peakHour = null,
        upiAmount = totalIncome,
        bankAmount = 0.0,
        newCustomers = newCustomers,
        returningCustomers = returningCustomers,
        hourlyStats = hourlyStats,
        weeklyTrend = weeklyTrend,
    )
}
