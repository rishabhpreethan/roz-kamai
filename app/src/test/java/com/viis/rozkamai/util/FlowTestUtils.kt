package com.viis.rozkamai.util

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow

/**
 * Extension function for testing Flows with Turbine.
 * Provides a cleaner API for asserting Flow emissions in tests.
 */
suspend fun <T> Flow<T>.testCollect(
    block: suspend TurbineTestContext<T>.() -> Unit,
) = test(block = block)
