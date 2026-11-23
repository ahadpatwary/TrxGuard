package com.txnrelay.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** Is "Notification access" granted to this app? */
class NotificationAccess(private val context: Context) {
    fun isEnabled(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
