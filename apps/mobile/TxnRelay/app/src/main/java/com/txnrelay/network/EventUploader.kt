package com.txnrelay.network

import com.txnrelay.data.db.PendingEventEntity
import com.txnrelay.data.settings.AppSettings
import kotlinx.coroutines.CancellationException
import java.io.IOException

sealed interface UploadResult {
    data object Success : UploadResult

    /** Network error, timeout, 5xx, 429… keep PENDING and retry later. */
    data class Retryable(val reason: String) : UploadResult

    /** Backend rejected this specific event (e.g. 400/422). Park it as FAILED_PERMANENT. */
    data class Permanent(val reason: String) : UploadResult

    /** Problem is our configuration (bad URL, 401/403/404). Keep everything PENDING, stop, tell the user. */
    data class InvalidConfig(val reason: String) : UploadResult
}

/** Abstraction over "send one event". Implement this to use another HTTP client or protocol. */
interface EventUploader {
    suspend fun upload(event: PendingEventEntity, settings: AppSettings): UploadResult
}

class RetrofitEventUploader(private val api: EventApi) : EventUploader {

    override suspend fun upload(event: PendingEventEntity, settings: AppSettings): UploadResult =
        try {
            val response = api.postEvent(
                url = settings.endpointUrl.trim(),
                idempotencyKey = event.eventId,
                authorization = settings.apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" },
                payload = event.toPayload(settings.deviceId),
            )
            classify(response.code())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            UploadResult.Retryable("Network error: ${e.message ?: e.javaClass.simpleName}")
        } catch (e: IllegalArgumentException) {
            // Retrofit throws this for a malformed @Url.
            UploadResult.InvalidConfig("Invalid endpoint URL")
        } catch (e: Exception) {
            UploadResult.Retryable("Unexpected error: ${e.javaClass.simpleName}")
        }

    /** Maps HTTP status codes to what the queue should do. Adjust to match your backend. */
    private fun classify(code: Int): UploadResult = when {
        code in 200..299 -> UploadResult.Success
        code == 409 -> UploadResult.Success // backend already has this eventId (idempotent replay)
        code == 401 || code == 403 -> UploadResult.InvalidConfig("Unauthorized (HTTP $code) – check API key")
        code == 404 -> UploadResult.InvalidConfig("Endpoint not found (HTTP 404) – check URL")
        code == 408 || code == 425 || code == 429 -> UploadResult.Retryable("HTTP $code")
        code in 500..599 -> UploadResult.Retryable("Server error (HTTP $code)")
        else -> UploadResult.Permanent("Rejected by server (HTTP $code)")
    }
}
