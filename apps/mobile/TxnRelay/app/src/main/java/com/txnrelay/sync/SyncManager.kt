package com.txnrelay.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.txnrelay.config.AppConfig
import com.txnrelay.data.EventRepository
import com.txnrelay.data.settings.SettingsStore
import java.util.concurrent.TimeUnit

/**
 * Decides WHEN to sync: immediately for new events, via WorkManager for retries / manual sync.
 * (SyncEngine decides HOW.)
 */
class SyncManager(
    context: Context,
    private val engine: SyncEngine,
    private val repository: EventRepository,
    private val settingsStore: SettingsStore,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** Called after a NEW event was stored: try right away, fall back to the retry queue. */
    suspend fun onEventCaptured() {
        if (engine.flush(force = false) == FlushResult.RETRY_LATER) {
            // KEEP: don't reset an already scheduled backoff.
            enqueue(force = false, policy = ExistingWorkPolicy.KEEP)
        }
    }

    /** "Sync now" button. REPLACE so it also resets any long backoff delay. */
    fun syncNow() = enqueue(force = true, policy = ExistingWorkPolicy.REPLACE)

    /** Settings changed (e.g. endpoint fixed) → flush whatever is waiting. */
    suspend fun onSettingsChanged() {
        if (shouldSync() && repository.hasPending()) {
            enqueue(force = false, policy = ExistingWorkPolicy.REPLACE)
        }
    }

    /** Safety net on app/service start: make sure queued events don't sit idle. */
    suspend fun scheduleIfPending() {
        if (shouldSync() && repository.hasPending()) {
            enqueue(force = false, policy = ExistingWorkPolicy.KEEP)
        }
    }

    private fun shouldSync(): Boolean = settingsStore.current.let { it.autoSync && it.isConfigured }

    private fun enqueue(force: Boolean, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, AppConfig.BACKOFF_INITIAL_SECONDS, TimeUnit.SECONDS)
            .setInputData(workDataOf(SyncWorker.KEY_FORCE to force))
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "txnrelay-sync"
    }
}
