package com.viis.rozkamai.infrastructure.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Receives incoming SMS broadcasts.
 * Filters to financial SMS only (by sender ID via SmsSenderFilter).
 * Enqueues SmsProcessingWorker via WorkManager — never does processing inline.
 *
 * Privacy rule: only logs first 4 chars of sender + "***". Never logs body.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        messages.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress ?: return@forEach
            val body = smsMessage.messageBody ?: return@forEach

            if (!SmsSenderFilter.isFinancialSender(sender)) {
                Timber.d("SMS skipped — non-financial sender: ${sender.take(4)}***")
                return@forEach
            }

            Timber.d("Financial SMS received from: ${sender.take(4)}***")

            val inputData = Data.Builder()
                .putString(SmsProcessingWorker.KEY_SENDER, sender)
                .putString(SmsProcessingWorker.KEY_BODY, body)
                .putLong(SmsProcessingWorker.KEY_RECEIVED_AT, System.currentTimeMillis())
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SmsProcessingWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Timber.d("SmsProcessingWorker enqueued for sender: ${sender.take(4)}***")
        }
    }
}
