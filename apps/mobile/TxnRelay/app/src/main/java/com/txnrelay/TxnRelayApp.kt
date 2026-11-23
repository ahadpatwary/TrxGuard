package com.txnrelay

import android.app.Application
import com.txnrelay.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TxnRelayApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        container.appScope.launch(Dispatchers.IO) {
            container.settings.current // warm up encrypted prefs off the main thread
            container.syncManager.scheduleIfPending()
        }
    }
}
