package com.viis.rozkamai.presentation.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viis.rozkamai.R

@Composable
fun SmsPermissionScreen(
    onPermissionGranted: () -> Unit,
    viewModel: SmsPermissionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        activity?.let { viewModel.onPermissionResult(granted, it) }
    }

    LaunchedEffect(state) {
        if (state is SmsPermissionState.Granted) onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        activity?.let { viewModel.checkPermission(it) }
    }

    when (state) {
        is SmsPermissionState.Granted -> Unit // navigated away by LaunchedEffect

        is SmsPermissionState.NeedsRequest -> PermissionContent(
            title = stringResource(R.string.permission_sms_title),
            body = stringResource(R.string.permission_sms_body),
            buttonLabel = stringResource(R.string.permission_sms_grant_button),
            onButtonClick = { permissionLauncher.launch(android.Manifest.permission.RECEIVE_SMS) },
        )

        is SmsPermissionState.NeedsRationale -> PermissionContent(
            title = stringResource(R.string.permission_sms_rationale_title),
            body = stringResource(R.string.permission_sms_rationale_body),
            buttonLabel = stringResource(R.string.permission_sms_try_again_button),
            onButtonClick = { permissionLauncher.launch(android.Manifest.permission.RECEIVE_SMS) },
        )

        is SmsPermissionState.PermanentlyDenied -> PermissionContent(
            title = stringResource(R.string.permission_sms_denied_title),
            body = stringResource(R.string.permission_sms_denied_body),
            buttonLabel = stringResource(R.string.permission_settings_button),
            onButtonClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    },
                )
            },
        )
    }
}

@Composable
private fun PermissionContent(
    title: String,
    body: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onButtonClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = buttonLabel)
        }
    }
}
