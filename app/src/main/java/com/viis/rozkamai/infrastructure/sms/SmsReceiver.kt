package com.viis.rozkamai.infrastructure.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives incoming SMS broadcasts.
 * Filters to financial SMS only (by sender ID prefix).
 * Delegates parsing to SmsParser via ParseSmsUseCase.
 *
 * This is a stub — full implementation in P1-001.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress ?: return@forEach
            val body = smsMessage.messageBody ?: return@forEach
            Timber.d("SMS received from: ${sender.take(4)}***") // Never log full sender
        }
    }
}
