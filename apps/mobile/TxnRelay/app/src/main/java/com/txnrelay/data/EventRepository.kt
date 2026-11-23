package com.txnrelay.data

import com.txnrelay.config.AppConfig
import com.txnrelay.data.db.PendingEventDao
import com.txnrelay.data.db.PendingEventEntity
import com.txnrelay.data.model.CapturedNotification
import com.txnrelay.data.model.EventStatus
import com.txnrelay.data.model.StatusCounts
import com.txnrelay.domain.EventIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class EnqueueResult { INSERTED, DUPLICATE }

/** Single entry point to the local queue. UI, listener and sync code all go through here. */
class EventRepository(
    private val dao: PendingEventDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // Serialises "check for duplicate, then insert" so concurrent callbacks can't both pass the check.
    private val enqueueLock = Mutex()

    /** Saves the notification unless it is a duplicate. */
    suspend fun enqueue(n: CapturedNotification): EnqueueResult = enqueueLock.withLock {
        val now = clock()
        val fingerprint = EventIdGenerator.fingerprint(n)

        // 1) Same content seen very recently (update/re-post of the same notification)?
        if (dao.countRecentByFingerprint(fingerprint, now - AppConfig.DEDUP_WINDOW_MS) > 0) {
            return@withLock EnqueueResult.DUPLICATE
        }

        // 2) Exact same event already stored? (primary key + IGNORE)
        val entity = PendingEventEntity(
            eventId = EventIdGenerator.eventId(n),
            fingerprint = fingerprint,
            notificationKey = n.notificationKey,
            packageName = n.packageName,
            title = n.title,
            body = n.body,
            postedAt = n.postedAt,
            createdAt = now,
        )
        if (dao.insert(entity) == -1L) EnqueueResult.DUPLICATE else EnqueueResult.INSERTED
    }

    suspend fun pendingBatch(limit: Int): List<PendingEventEntity> = dao.getPending(limit)
    suspend fun hasPending(): Boolean = dao.countPending() > 0
    suspend fun markSent(eventId: String) = dao.markSent(eventId, clock())
    suspend fun markFailedPermanent(eventId: String, error: String) = dao.markFailedPermanent(eventId, error)
    suspend fun requeueFailed() = dao.requeueFailed()

    /** Records a transient failure; gives up (FAILED_PERMANENT) after [AppConfig.MAX_RETRY_COUNT]. */
    suspend fun recordRetry(event: PendingEventEntity, error: String) {
        if (event.retryCount + 1 >= AppConfig.MAX_RETRY_COUNT) {
            dao.markFailedPermanent(event.eventId, "Gave up after ${AppConfig.MAX_RETRY_COUNT} attempts: $error")
        } else {
            dao.recordRetry(event.eventId, error)
        }
    }

    /** Deletes old SENT rows. */
    suspend fun cleanup() {
        dao.deleteSentBefore(clock() - AppConfig.SENT_RETENTION_MS)
    }

    fun observeRecent(limit: Int = AppConfig.RECENT_EVENTS_LIMIT): Flow<List<PendingEventEntity>> =
        dao.observeRecent(limit)

    fun observeCounts(): Flow<StatusCounts> = combine(
        dao.observeCount(EventStatus.PENDING),
        dao.observeCount(EventStatus.SENT),
        dao.observeCount(EventStatus.FAILED_PERMANENT),
    ) { pending, sent, failed -> StatusCounts(pending, sent, failed) }
}
