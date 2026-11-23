package com.txnrelay.network

import com.txnrelay.config.TargetApps
import com.txnrelay.data.db.PendingEventEntity
import kotlinx.serialization.Serializable

/**
 * JSON body sent to the backend. To add/rename/remove fields, edit this class and [toPayload].
 *
 * Example:
 * {
 *   "eventId": "9f2c…",        // sha256, also sent as the Idempotency-Key header
 *   "source": "bkash",         // "bkash" | "nagad"
 *   "packageName": "com.bkash.customerapp",
 *   "title": "…", "body": "…",
 *   "postedAt": 1732400000000, // notification post time (epoch ms)
 *   "capturedAt": 1732400000500,
 *   "deviceId": "uuid-per-install"
 * }
 */
@Serializable
data class EventPayload(
    val eventId: String,
    val source: String,
    val packageName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val capturedAt: Long,
    val deviceId: String,
)

fun PendingEventEntity.toPayload(deviceId: String) = EventPayload(
    eventId = eventId,
    source = TargetApps.fromPackage(packageName)?.id ?: "unknown",
    packageName = packageName,
    title = title,
    body = body,
    postedAt = postedAt,
    capturedAt = createdAt,
    deviceId = deviceId,
)
