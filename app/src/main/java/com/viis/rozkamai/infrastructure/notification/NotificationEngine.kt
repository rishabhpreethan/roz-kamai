package com.viis.rozkamai.infrastructure.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.viis.rozkamai.R
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import com.viis.rozkamai.infrastructure.worker.EodContent
import com.viis.rozkamai.infrastructure.worker.MidDayAlertContent
import com.viis.rozkamai.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Sends notifications for all three VIIS alert types and appends
 * a NotificationSent event to the event store after each send (P3-008).
 *
 * Single notification ID per type — repeated calls update rather than stack.
 * All text is Hinglish (P3-006). Tapping opens MainActivity (P3-007).
 */
@Singleton
class NotificationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository,
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val indianLocale = Locale("en", "IN")

    // ─── Public send methods ──────────────────────────────────────────────────

    /** P3-003: EOD summary at ~9 PM showing today's income and day-over-day delta. */
    suspend fun sendEodSummary(content: EodContent) {
        if (!hasPermission()) return
        val body = buildEodBody(content)
        notify(
            id = NOTIF_ID_EOD,
            channelId = NotificationChannels.EOD_SUMMARY,
            title = context.getString(R.string.notif_eod_title),
            body = body,
        )
        appendEvent("EodSummary")
        Timber.d("NotificationEngine: sent EodSummary")
    }

    /** P3-004: Mid-day alert when income is below 70% of expected baseline. */
    suspend fun sendMidDayAlert(content: MidDayAlertContent) {
        if (!hasPermission()) return
        val body = context.getString(
            R.string.notif_midday_body,
            formatAmount(content.currentIncome),
            formatAmount(content.expectedIncome),
        )
        notify(
            id = NOTIF_ID_MIDDAY,
            channelId = NotificationChannels.MIDDAY_ALERT,
            title = context.getString(R.string.notif_midday_title),
            body = body,
        )
        appendEvent("MidDayAlert")
        Timber.d("NotificationEngine: sent MidDayAlert")
    }

    /** P3-005: Inactivity alert when no transaction for [gapMinutes] minutes. */
    suspend fun sendInactivityAlert(gapMinutes: Long) {
        if (!hasPermission()) return
        val gapHours = (gapMinutes / 60.0).roundToInt().coerceAtLeast(1)
        val body = context.getString(R.string.notif_inactivity_body, gapHours)
        notify(
            id = NOTIF_ID_INACTIVITY,
            channelId = NotificationChannels.INACTIVITY,
            title = context.getString(R.string.notif_inactivity_title),
            body = body,
        )
        appendEvent("InactivityAlert")
        Timber.d("NotificationEngine: sent InactivityAlert (gap=${gapMinutes}min)")
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun notify(id: Int, channelId: String, title: String, body: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(id, notification)
    }

    private fun buildEodBody(content: EodContent): String {
        val dayOverDay = content.dayOverDayIncome
        return if (dayOverDay == null) {
            context.getString(
                R.string.notif_eod_body_plain,
                formatAmount(content.totalIncome),
                content.transactionCount,
            )
        } else {
            val diff = content.totalIncome - dayOverDay
            when {
                diff > 0.5 -> context.getString(
                    R.string.notif_eod_body_more,
                    formatAmount(content.totalIncome),
                    formatAmountPlain(abs(diff)),
                )
                diff < -0.5 -> context.getString(
                    R.string.notif_eod_body_less,
                    formatAmount(content.totalIncome),
                    formatAmountPlain(abs(diff)),
                )
                else -> context.getString(
                    R.string.notif_eod_body_same,
                    formatAmount(content.totalIncome),
                )
            }
        }
    }

    private suspend fun appendEvent(notifType: String) {
        val ts = System.currentTimeMillis()
        eventRepository.appendEvent(
            EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = "NotificationSent",
                timestamp = ts,
                payload = """{"type":"$notifType","sent_at":$ts}""",
                version = 1,
            ),
        )
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun formatAmount(amount: Double): String {
        val fmt = NumberFormat.getCurrencyInstance(indianLocale)
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }

    private fun formatAmountPlain(amount: Double): String {
        val fmt = NumberFormat.getNumberInstance(indianLocale)
        fmt.maximumFractionDigits = 0
        return fmt.format(amount)
    }

    companion object {
        const val NOTIF_ID_EOD = 1001
        const val NOTIF_ID_MIDDAY = 1002
        const val NOTIF_ID_INACTIVITY = 1003
    }
}
