package com.viis.rozkamai.util

import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base class for all unit tests.
 * Sets up a TestDispatcher for coroutines and cleans up MockK mocks after each test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseUnitTest {

    val testScheduler = TestCoroutineScheduler()
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Before
    open fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }
}
