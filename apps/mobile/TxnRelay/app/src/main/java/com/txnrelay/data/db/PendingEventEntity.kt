package com.txnrelay.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.txnrelay.data.model.EventStatus

/**
 * One captured notification, stored until the backend confirms it.
 * [eventId] is the primary key, so the same event can never be inserted twice.
 */
@Entity(
    tableName = "pending_events",
    indices = [
        Index(value = ["status", "postedAt"]),
        Index(value = ["fingerprint", "createdAt"]),
    ],
)
data class PendingEventEntity(
    @PrimaryKey val eventId: String,
    val fingerprint: String,
    val notificationKey: String,
    val packageName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val createdAt: Long,
    val status: EventStatus = EventStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val sentAt: Long? = null,
)
