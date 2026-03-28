package com.viis.rozkamai.infrastructure.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Re-registers any necessary components after device restart.
 * SMS BroadcastReceiver is registered in manifest (persistent),
 * so this primarily re-schedules WorkManager jobs.
 *
 * Full implementation in P1-020.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Boot completed — re-initializing background jobs")
        // WorkManager jobs will be re-scheduled here in P1-020
    }
}
