package com.viis.rozkamai.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.viis.rozkamai.infrastructure.sms.SmsPermissionManager
import com.viis.rozkamai.presentation.permission.SmsPermissionScreen
import com.viis.rozkamai.presentation.theme.RozKamaiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionManager: SmsPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RozKamaiTheme {
                if (!permissionManager.hasPermission()) {
                    SmsPermissionScreen(
                        onPermissionGranted = { recreate() },
                    )
                }
                // Main navigation host goes here (Phase 2)
            }
        }
    }
}
