package com.viis.rozkamai.presentation.permission

import android.app.Activity
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.infrastructure.sms.SmsPermissionManager
import com.viis.rozkamai.util.BaseUnitTest
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsPermissionViewModelTest : BaseUnitTest() {

    private lateinit var permissionManager: SmsPermissionManager
    private lateinit var eventRepository: EventRepository
    private lateinit var activity: Activity
    private lateinit var viewModel: SmsPermissionViewModel

    @Before
    override fun setUp() {
        super.setUp()
        permissionManager = mockk(relaxed = true)
        eventRepository = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        viewModel = SmsPermissionViewModel(permissionManager, eventRepository)
    }

    @Test
    fun `checkPermission - hasPermission true - state is Granted`() {
        every { permissionManager.hasPermission() } returns true

        viewModel.checkPermission(activity)

        assertEquals(SmsPermissionState.Granted, viewModel.state.value)
    }

    @Test
    fun `checkPermission - hasPermission false, no rationale - state is NeedsRequest`() {
        every { permissionManager.hasPermission() } returns false
        every { permissionManager.shouldShowRationale(activity) } returns false

        viewModel.checkPermission(activity)

        assertEquals(SmsPermissionState.NeedsRequest, viewModel.state.value)
    }

    @Test
    fun `checkPermission - hasPermission false, shouldShowRationale true - state is NeedsRationale`() {
        every { permissionManager.hasPermission() } returns false
        every { permissionManager.shouldShowRationale(activity) } returns true

        viewModel.checkPermission(activity)

        assertEquals(SmsPermissionState.NeedsRationale, viewModel.state.value)
    }

    @Test
    fun `onPermissionResult granted true - state is Granted`() = runTest {
        every { permissionManager.hasPermission() } returns true

        viewModel.onPermissionResult(granted = true, activity = activity)

        assertEquals(SmsPermissionState.Granted, viewModel.state.value)
    }

    @Test
    fun `onPermissionResult granted true - PermissionGranted event appended`() = runTest {
        viewModel.onPermissionResult(granted = true, activity = activity)

        coVerify {
            eventRepository.appendEvent(
                match { event: EventEntity ->
                    event.eventType == "PermissionGranted"
                },
            )
        }
    }

    @Test
    fun `onPermissionResult granted false, shouldShowRationale false - state is PermanentlyDenied`() = runTest {
        every { permissionManager.shouldShowRationale(activity) } returns false

        viewModel.onPermissionResult(granted = false, activity = activity)

        assertEquals(SmsPermissionState.PermanentlyDenied, viewModel.state.value)
    }

    @Test
    fun `onPermissionResult granted false, shouldShowRationale true - state is NeedsRationale`() = runTest {
        every { permissionManager.shouldShowRationale(activity) } returns true

        viewModel.onPermissionResult(granted = false, activity = activity)

        assertEquals(SmsPermissionState.NeedsRationale, viewModel.state.value)
    }

    @Test
    fun `onPermissionResult granted false - PermissionDenied event appended`() = runTest {
        every { permissionManager.shouldShowRationale(activity) } returns false

        viewModel.onPermissionResult(granted = false, activity = activity)

        coVerify {
            eventRepository.appendEvent(
                match { event: EventEntity ->
                    event.eventType == "PermissionDenied"
                },
            )
        }
    }

    @Test
    fun `initial state is NeedsRequest`() {
        assertTrue(viewModel.state.value is SmsPermissionState.NeedsRequest)
    }
}
