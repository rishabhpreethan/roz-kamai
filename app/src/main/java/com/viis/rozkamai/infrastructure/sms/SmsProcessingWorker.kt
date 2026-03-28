package com.viis.rozkamai.infrastructure.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.viis.rozkamai.data.local.entity.EventEntity
import com.viis.rozkamai.data.repository.EventRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.UUID

/**
 * Background worker that records a SMSReceived event to the event store.
 * Receives sender + body via WorkManager input data.
 * Writes only privacy-safe fields to the event payload (masked sender, body length).
 */
@HiltWorker
class SmsProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val eventRepository: EventRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SENDER = "key_sender"
        const val KEY_BODY = "key_body"
        const val KEY_RECEIVED_AT = "key_received_at"

        private const val EVENT_TYPE_SMS_RECEIVED = "SMSReceived"
    }

    override suspend fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val receivedAt = inputData.getLong(KEY_RECEIVED_AT, System.currentTimeMillis())

        val senderMasked = "${sender.take(4)}***"

        Timber.d("SmsProcessingWorker: processing SMS from $senderMasked")

        return try {
            val payload = buildPrivacySafePayload(senderMasked, body.length)

            val event = EventEntity(
                eventId = UUID.randomUUID().toString(),
                eventType = EVENT_TYPE_SMS_RECEIVED,
                timestamp = receivedAt,
                payload = payload,
            )

            eventRepository.appendEvent(event)
            Timber.d("SMSReceived event appended for sender $senderMasked")

            Result.success()
        } catch (e: Exception) {
            Timber.e("SmsProcessingWorker: failed to append event — ${e.javaClass.simpleName}")
            Result.retry()
        }
    }

    private fun buildPrivacySafePayload(senderMasked: String, bodyLength: Int): String {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(Map::class.java)
        return adapter.toJson(
            mapOf(
                "sender_masked" to senderMasked,
                "body_length" to bodyLength,
            ),
        )
    }
}
