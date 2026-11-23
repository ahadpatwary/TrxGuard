package com.txnrelay.listener

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.txnrelay.TxnRelayApp
import com.txnrelay.config.TargetApps
import com.txnrelay.data.EnqueueResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Entry point for notifications. Keeps the callbacks tiny: filter → parse → hand off to a coroutine
 * that stores the event in Room and triggers sync. No blocking work happens on the main thread.
 */
class PaymentNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container get() = (application as TxnRelayApp).container

    override fun onListenerConnected() {
        super.onListenerConnected()

        // One-time sweep of notifications that are already showing (posted while we weren't connected).
        // Duplicates are rejected by the repository, so running this on every reconnect is safe.
        val active = try {
            activeNotifications?.toList().orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "getActiveNotifications failed", e)
            emptyList()
        }
        active.forEach(::handle)

        scope.launch { container.syncManager.scheduleIfPending() }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) handle(sbn)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Ask the system to bind us again (helps after OEM "optimisers" or process kills).
        try {
            NotificationListenerService.requestRebind(ComponentName(this, PaymentNotificationListener::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "requestRebind failed", e)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun handle(sbn: StatusBarNotification) {
        // Cheapest check first: ignore everything that isn't bKash/Nagad.
        if (TargetApps.fromPackage(sbn.packageName) == null) return
        val captured = NotificationParser.parse(sbn) ?: return

        scope.launch {
            try {
                val c = container
                if (c.repository.enqueue(captured) == EnqueueResult.INSERTED) {
                    c.syncManager.onEventCaptured()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification", e)
            }
        }
    }

    private companion object {
        const val TAG = "PaymentListener"
    }
}
