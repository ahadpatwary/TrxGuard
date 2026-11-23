package com.txnrelay.di

import android.content.Context
import com.txnrelay.data.EventRepository
import com.txnrelay.data.db.AppDatabase
import com.txnrelay.data.settings.SettingsStore
import com.txnrelay.network.EventUploader
import com.txnrelay.network.RetrofitEventUploader
import com.txnrelay.network.createEventApi
import com.txnrelay.sync.SyncEngine
import com.txnrelay.sync.SyncManager
import com.txnrelay.util.NotificationAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Tiny hand-written dependency container (no DI framework needed for an app this size).
 * Everything is lazy so process start stays fast. Swap implementations here (e.g. a different EventUploader).
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    val repository: EventRepository by lazy { EventRepository(database.pendingEventDao()) }
    private val uploader: EventUploader by lazy { RetrofitEventUploader(createEventApi()) }
    val syncEngine: SyncEngine by lazy { SyncEngine(repository, uploader, settings) }
    val syncManager: SyncManager by lazy { SyncManager(appContext, syncEngine, repository, settings) }
    val notificationAccess: NotificationAccess by lazy { NotificationAccess(appContext) }
}
