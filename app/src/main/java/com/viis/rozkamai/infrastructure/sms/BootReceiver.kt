package com.viis.rozkamai.infrastructure.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.viis.rozkamai.infrastructure.worker.WorkScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

/**
 * Fires on BOOT_COMPLETED and MY_PACKAGE_REPLACED to ensure background
 * infrastructure is live after a device restart or app update (P3-009).
 *
 * SMS monitoring: SmsReceiver is registered in the manifest, so it resumes
 * automatically — no explicit re-registration needed.
 *
 * WorkManager: re-schedules all periodic workers so that EOD summary, mid-day
 * alert, and idle detection continue firing after a reboot. WorkManager's KEEP
 * policy prevents duplicates.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun workScheduler(): WorkScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val workScheduler = EntryPointAccessors
            .fromApplication(context.applicationContext, BootReceiverEntryPoint::class.java)
            .workScheduler()
        workScheduler.schedulePeriodicWorkers()

        Timber.d("BootReceiver: rescheduled all periodic workers")
    }
}
