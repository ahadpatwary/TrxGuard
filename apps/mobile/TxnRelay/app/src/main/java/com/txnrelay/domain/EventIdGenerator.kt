package com.txnrelay.domain

import com.txnrelay.data.model.CapturedNotification
import java.security.MessageDigest

/**
 * Deterministic IDs used for deduplication and as the backend idempotency key.
 *
 * - [eventId]: identifies one concrete posting (package + content + post time).
 *   Re-reading the same active notification (e.g. after the listener reconnects) yields
 *   the same id, so Room's primary key rejects it.
 * - [fingerprint]: content only (no time). Used for the short "same text re-posted/updated"
 *   window check in EventRepository.
 */
object EventIdGenerator {

    fun eventId(n: CapturedNotification): String =
        sha256("${n.packageName}|${n.postedAt}|${n.title}|${n.body}")

    fun fingerprint(n: CapturedNotification): String =
        sha256("${n.packageName}|${n.title}|${n.body}")

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
