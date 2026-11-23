package com.txnrelay.listener

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import com.txnrelay.config.AppConfig
import com.txnrelay.data.model.CapturedNotification

/** Turns a StatusBarNotification into a [CapturedNotification]; never throws. */
object NotificationParser {

    private const val TAG = "NotificationParser"

    /** Returns null when there is nothing useful to capture (no title and no text). */
    fun parse(sbn: StatusBarNotification): CapturedNotification? {
        return try {
            val extras = sbn.notification?.extras ?: return null

            val title = extras.text(Notification.EXTRA_TITLE) ?: extras.text(Notification.EXTRA_TITLE_BIG)

            // BIG_TEXT holds the full message when the normal TEXT is truncated ("…"). Keep the longest.
            val body = listOfNotNull(
                extras.text(Notification.EXTRA_BIG_TEXT),
                extras.text(Notification.EXTRA_TEXT),
                extras.lines(Notification.EXTRA_TEXT_LINES),
                extras.text(Notification.EXTRA_SUB_TEXT),
            ).maxByOrNull { it.length }

            if (title == null && body == null) return null

            CapturedNotification(
                notificationKey = sbn.key ?: "${sbn.packageName}:${sbn.id}",
                packageName = sbn.packageName,
                title = title.orEmpty().take(AppConfig.MAX_TITLE_LENGTH),
                body = body.orEmpty().take(AppConfig.MAX_BODY_LENGTH),
                postedAt = sbn.postTime,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse notification from ${sbn.packageName}", e)
            null
        }
    }

    private fun Bundle.text(key: String): String? =
        getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun Bundle.lines(key: String): String? =
        getCharSequenceArray(key)?.joinToString("\n") { it.toString().trim() }?.takeIf { it.isNotBlank() }
}
