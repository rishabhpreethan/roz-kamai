package com.viis.rozkamai.presentation.permission

sealed class SmsPermissionState {
    object Granted : SmsPermissionState()
    object NeedsRequest : SmsPermissionState()
    object NeedsRationale : SmsPermissionState()
    object PermanentlyDenied : SmsPermissionState()
}
