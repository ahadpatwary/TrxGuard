package com.txnrelay.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.txnrelay.TxnRelayApp
import kotlinx.coroutines.CancellationException

/**
 * Background retry job. Runs only when the network is available (see SyncManager constraints).
 * Result.retry() makes WorkManager re-run it with exponential backoff.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val engine = (applicationContext as TxnRelayApp).container.syncEngine
        val force = inputData.getBoolean(KEY_FORCE, false)
        return try {
            when (engine.flush(force)) {
                FlushResult.RETRY_LATER -> Result.retry()
                FlushResult.DONE, FlushResult.BLOCKED, FlushResult.SKIPPED -> Result.success()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
