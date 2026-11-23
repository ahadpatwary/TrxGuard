package com.txnrelay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.txnrelay.data.model.EventStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingEventDao {

    /** Returns the new rowId, or -1 if [PendingEventEntity.eventId] already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PendingEventEntity): Long

    @Query("SELECT COUNT(*) FROM pending_events WHERE fingerprint = :fingerprint AND createdAt >= :since")
    suspend fun countRecentByFingerprint(fingerprint: String, since: Long): Int

    @Query("SELECT * FROM pending_events WHERE status = 'PENDING' ORDER BY postedAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<PendingEventEntity>

    @Query("SELECT COUNT(*) FROM pending_events WHERE status = 'PENDING'")
    suspend fun countPending(): Int

    @Query("UPDATE pending_events SET status = 'SENT', sentAt = :now, lastError = NULL WHERE eventId = :eventId")
    suspend fun markSent(eventId: String, now: Long)

    @Query("UPDATE pending_events SET status = 'FAILED_PERMANENT', lastError = :error WHERE eventId = :eventId")
    suspend fun markFailedPermanent(eventId: String, error: String)

    @Query("UPDATE pending_events SET retryCount = retryCount + 1, lastError = :error WHERE eventId = :eventId")
    suspend fun recordRetry(eventId: String, error: String)

    /** Used by "Sync now": give permanently failed events another chance. */
    @Query("UPDATE pending_events SET status = 'PENDING', retryCount = 0 WHERE status = 'FAILED_PERMANENT'")
    suspend fun requeueFailed(): Int

    @Query("DELETE FROM pending_events WHERE status = 'SENT' AND sentAt < :cutoff")
    suspend fun deleteSentBefore(cutoff: Long): Int

    @Query("SELECT * FROM pending_events ORDER BY postedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PendingEventEntity>>

    @Query("SELECT COUNT(*) FROM pending_events WHERE status = :status")
    fun observeCount(status: EventStatus): Flow<Int>
}
