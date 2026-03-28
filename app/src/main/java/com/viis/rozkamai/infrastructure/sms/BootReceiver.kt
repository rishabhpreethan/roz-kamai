package com.viis.rozkamai.infrastructure.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import timber.log.Timber

/**
 * Fires on BOOT_COMPLETED to ensure background infrastructure is live after a restart.
 *
 * SMS monitoring: SmsReceiver is registered in the manifest, so it resumes automatically —
 * no explicit re-registration is needed.
 *
 * WorkManager: initializes the WorkManager instance so it can accept new SMS jobs immediately.
 * Any in-flight one-shot workers that didn't complete before shutdown are retried automatically
 * by WorkManager's own persistence layer.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Touch WorkManager to initialize it — ensures the first SMS after boot is processed
        // without a cold-start delay in the WorkManager internal scheduler.
        WorkManager.getInstance(context)
        Timber.d("BootReceiver: device booted, SMS monitoring active via manifest registration")
    }
}
