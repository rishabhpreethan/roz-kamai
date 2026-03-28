package com.viis.rozkamai.presentation.permission

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.infrastructure.sms.SmsPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SmsPermissionViewModel @Inject constructor(
    private val permissionManager: SmsPermissionManager,
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SmsPermissionState>(SmsPermissionState.NeedsRequest)
    val state: StateFlow<SmsPermissionState> = _state.asStateFlow()

    fun checkPermission(activity: Activity) {
        _state.value = when {
            permissionManager.hasPermission() -> SmsPermissionState.Granted
            permissionManager.shouldShowRationale(activity) -> SmsPermissionState.NeedsRationale
            else -> SmsPermissionState.NeedsRequest
        }
    }

    fun onPermissionResult(granted: Boolean, activity: Activity) {
        if (granted) {
            _state.value = SmsPermissionState.Granted
            appendPermissionEvent("RECEIVE_SMS", granted = true, isPermanent = false)
            Timber.d("SMS permission granted")
        } else {
            val permanent = !permissionManager.shouldShowRationale(activity)
            _state.value = if (permanent) {
                SmsPermissionState.PermanentlyDenied
            } else {
                SmsPermissionState.NeedsRationale
            }
            appendPermissionEvent("RECEIVE_SMS", granted = false, isPermanent = permanent)
            Timber.d("SMS permission denied (permanent=$permanent)")
        }
    }

    private fun appendPermissionEvent(permission: String, granted: Boolean, isPermanent: Boolean) {
        viewModelScope.launch {
            val eventType = if (granted) "PermissionGranted" else "PermissionDenied"
            val payload = if (granted) {
                """{"permission":"$permission","granted_at":${System.currentTimeMillis()}}"""
            } else {
                """{"permission":"$permission","is_permanent":$isPermanent,"denied_at":${System.currentTimeMillis()}}"""
            }
            eventRepository.appendEvent(
                EventEntity(
                    eventId = UUID.randomUUID().toString(),
                    eventType = eventType,
                    timestamp = System.currentTimeMillis(),
                    payload = payload,
                    version = 1,
                ),
            )
        }
    }
}
