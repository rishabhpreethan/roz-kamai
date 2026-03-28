package com.viis.rozkamai.infrastructure.sms

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    fun shouldShowRationale(activity: Activity): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECEIVE_SMS)
}
