package com.viis.rozkamai.infrastructure.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.viis.rozkamai.domain.parser.ParseResult
import com.viis.rozkamai.domain.usecase.ParseSmsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker that processes an incoming financial SMS.
 * Delegates to ParseSmsUseCase which handles event production.
 * This worker logs only privacy-safe information (masked sender, result type).
 */
@HiltWorker
class SmsProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val parseSmsUseCase: ParseSmsUseCase,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SENDER = "key_sender"
        const val KEY_BODY = "key_body"
        const val KEY_RECEIVED_AT = "key_received_at"
    }

    override suspend fun doWork(): Result {
        val sender = inputData.getString(KEY_SENDER) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val receivedAt = inputData.getLong(KEY_RECEIVED_AT, System.currentTimeMillis())

        val senderMasked = "${sender.take(4)}***"
        Timber.d("SmsProcessingWorker: processing SMS from $senderMasked")

        return try {
            when (val result = parseSmsUseCase.execute(sender, body, receivedAt)) {
                is ParseResult.Success -> {
                    Timber.d("SmsProcessingWorker: parsed successfully via ${result.parserSource}")
                    Result.success()
                }
                is ParseResult.Failed -> {
                    Timber.d("SmsProcessingWorker: parse failed — ${result.reason}")
                    Result.success() // failure is expected for unknown formats; don't retry
                }
                is ParseResult.Duplicate -> {
                    Timber.d("SmsProcessingWorker: duplicate detected, skipping")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Timber.e("SmsProcessingWorker: unexpected error — ${e.javaClass.simpleName}")
            Result.retry()
        }
    }
}
