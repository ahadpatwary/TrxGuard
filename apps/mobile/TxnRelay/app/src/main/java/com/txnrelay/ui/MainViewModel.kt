package com.txnrelay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.txnrelay.TxnRelayApp
import com.txnrelay.config.PaymentApp
import com.txnrelay.config.TargetApps
import com.txnrelay.data.EnqueueResult
import com.txnrelay.data.model.CapturedNotification
import com.txnrelay.data.settings.ThemeMode
import com.txnrelay.di.AppContainer
import com.txnrelay.util.UrlValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Holds UI state only. All real work is delegated to repository / sync classes. */
class MainViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository
    private val settingsStore = container.settings
    private val syncManager = container.syncManager
    private val engine = container.syncEngine
    private val notificationAccess = container.notificationAccess

    private val accessEnabled = MutableStateFlow(notificationAccess.isEnabled())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** One-off messages for the snackbar. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var testCounter = 0

    val uiState: StateFlow<MainUiState> = combine(
        combine(repository.observeRecent(), repository.observeCounts()) { events, counts -> events to counts },
        combine(settingsStore.settings, engine.isSyncing, engine.lastError) { s, syncing, error ->
            Triple(s, syncing, error)
        },
        accessEnabled,
    ) { (events, counts), (settings, syncing, error), access ->
        MainUiState(
            accessEnabled = access,
            counts = counts,
            events = events.map {
                EventUi(
                    id = it.eventId,
                    app = TargetApps.fromPackage(it.packageName),
                    title = it.title,
                    body = it.body,
                    postedAt = it.postedAt,
                    status = it.status,
                    retryCount = it.retryCount,
                    lastError = it.lastError,
                )
            },
            settings = settings,
            isSyncing = syncing,
            lastSyncError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(settings = settingsStore.current, accessEnabled = accessEnabled.value),
    )

    /** Call from ON_RESUME: the user may have toggled notification access in system settings. */
    fun refreshAccess() {
        accessEnabled.value = notificationAccess.isEnabled()
    }

    fun saveConnection(endpoint: String, apiKey: String) {
        val url = endpoint.trim()
        if (!UrlValidator.isValid(url)) {
            _messages.tryEmit("Enter a valid https:// URL")
            return
        }
        settingsStore.update { it.copy(endpointUrl = url, apiKey = apiKey.trim()) }
        _messages.tryEmit("Connection settings saved")
        viewModelScope.launch { syncManager.onSettingsChanged() }
    }

    fun setAutoSync(enabled: Boolean) {
        settingsStore.update { it.copy(autoSync = enabled) }
        if (enabled) viewModelScope.launch { syncManager.onSettingsChanged() }
    }

    fun setTheme(mode: ThemeMode) {
        settingsStore.update { it.copy(themeMode = mode) }
    }

    fun syncNow() {
        if (!settingsStore.current.isConfigured) {
            _messages.tryEmit("Set the API endpoint first")
            return
        }
        syncManager.syncNow()
        _messages.tryEmit("Sync started (waits for internet if offline)")
    }

    /** Inserts a fake bKash/Nagad event so the whole pipeline can be tried without real notifications. */
    fun addTestEvent() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val app = if (testCounter++ % 2 == 0) PaymentApp.BKASH else PaymentApp.NAGAD
            val sample = CapturedNotification(
                notificationKey = "test:$now",
                packageName = app.packages.first(),
                title = "[TEST] ${app.displayName}",
                body = "You have received Tk 500.00 from 01XXXXXXXXX. TrxID TEST$now",
                postedAt = now,
            )
            if (repository.enqueue(sample) == EnqueueResult.INSERTED) {
                syncManager.onEventCaptured()
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as TxnRelayApp
                MainViewModel(app.container)
            }
        }
    }
}
