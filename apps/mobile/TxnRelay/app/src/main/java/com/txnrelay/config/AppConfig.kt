package com.txnrelay.config

/** Central tunables. Change behaviour here instead of hunting through the code. */
object AppConfig {
    /**
     * Same package + title + body seen again within this window is treated as a duplicate
     * (apps often re-post/update the same notification several times).
     */
    const val DEDUP_WINDOW_MS = 2 * 60 * 1000L

    /** Payload size guards (keeps requests lightweight). */
    const val MAX_TITLE_LENGTH = 200
    const val MAX_BODY_LENGTH = 2_000

    /** How many queued events one sync pass loads at a time. */
    const val UPLOAD_BATCH_SIZE = 50

    /** After this many failed attempts an event is parked as FAILED_PERMANENT. */
    const val MAX_RETRY_COUNT = 100

    /** SENT rows older than this are deleted after a successful sync. */
    const val SENT_RETENTION_MS = 7L * 24 * 60 * 60 * 1000

    /** WorkManager exponential backoff starting delay (30s, 60s, 120s, ...). */
    const val BACKOFF_INITIAL_SECONDS = 30L

    /** HTTP timeouts. */
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_WRITE_TIMEOUT_SECONDS = 20L
    const val CALL_TIMEOUT_SECONDS = 30L

    /** How many rows the UI list shows. */
    const val RECENT_EVENTS_LIMIT = 100
}
