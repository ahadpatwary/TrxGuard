package com.txnrelay.data.model

enum class EventStatus {
    /** Stored locally, waiting to be uploaded (or retried). */
    PENDING,

    /** Backend accepted it. */
    SENT,

    /** Backend rejected it (4xx) or we gave up after too many attempts. */
    FAILED_PERMANENT,
}

/** What the listener extracts from a StatusBarNotification (all fields already null-safe). */
data class CapturedNotification(
    val notificationKey: String,
    val packageName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
)

data class StatusCounts(
    val pending: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
)
