package com.viis.rozkamai.infrastructure.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.viis.rozkamai.R

/**
 * Defines the three notification channels used by VIIS.
 * createAll() is called once from RozKamaiApp.onCreate() (P3-002).
 */
object NotificationChannels {

    const val EOD_SUMMARY = "viis_eod_summary"
    const val MIDDAY_ALERT = "viis_midday_alert"
    const val INACTIVITY = "viis_inactivity"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    EOD_SUMMARY,
                    context.getString(R.string.notif_channel_eod_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notif_channel_eod_desc)
                },
                NotificationChannel(
                    MIDDAY_ALERT,
                    context.getString(R.string.notif_channel_midday_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.notif_channel_midday_desc)
                },
                NotificationChannel(
                    INACTIVITY,
                    context.getString(R.string.notif_channel_inactivity_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notif_channel_inactivity_desc)
                },
            ),
        )
    }
}
