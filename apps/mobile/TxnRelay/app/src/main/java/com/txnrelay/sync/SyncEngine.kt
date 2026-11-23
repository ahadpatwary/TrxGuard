package com.txnrelay.sync

import com.txnrelay.config.AppConfig
import com.txnrelay.data.EventRepository
import com.txnrelay.data.settings.AppSettings
import com.txnrelay.data.settings.SettingsStore
import com.txnrelay.network.EventUploader
import com.txnrelay.network.UploadResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class FlushResult {
    /** Queue drained. */
    DONE,

    /** Transient failure – caller should schedule a retry (WorkManager backoff). */
    RETRY_LATER,

    /** Needs the user (endpoint/API key). Retrying automatically would be pointless. */
    BLOCKED,

    /** Auto-sync is off and this wasn't a manual sync. */
    SKIPPED,
}

/**
 * Uploads PENDING events oldest-first. Used by both the listener (immediate attempt) and
 * SyncWorker (retries). The mutex guarantees only one upload pass runs at a time, and the
 * eventId idempotency key protects against duplicates on the backend side.
 */
class SyncEngine(
    private val repository: EventRepository,
    private val uploader: EventUploader,
    private val settingsStore: SettingsStore,
) {
    private val lock = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Human readable reason for the last problem (shown in the UI), null when healthy. */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    suspend fun flush(force: Boolean): FlushResult = lock.withLock {
        val settings = settingsStore.current
        if (!force && !settings.autoSync) return@withLock FlushResult.SKIPPED
        if (!settings.isConfigured) {
            _lastError.value = "Set the API endpoint to start syncing"
            return@withLock FlushResult.BLOCKED
        }

        _isSyncing.value = true
        try {
            drain(settings, force)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun drain(settings: AppSettings, force: Boolean): FlushResult {
        if (force) repository.requeueFailed()

        while (true) {
            currentCoroutineContext().ensureActive()
            val batch = repository.pendingBatch(AppConfig.UPLOAD_BATCH_SIZE)
            if (batch.isEmpty()) break

            for (event in batch) {
                when (val result = uploader.upload(event, settings)) {
                    UploadResult.Success -> repository.markSent(event.eventId)
                    is UploadResult.Permanent -> repository.markFailedPermanent(event.eventId, result.reason)
                    is UploadResult.Retryable -> {
                        repository.recordRetry(event, result.reason)
                        _lastError.value = result.reason
                        return FlushResult.RETRY_LATER
                    }
                    is UploadResult.InvalidConfig -> {
                        _lastError.value = result.reason
                        return FlushResult.BLOCKED
                    }
                }
            }
        }

        _lastError.value = null
        repository.cleanup()
        return FlushResult.DONE
    }
}
