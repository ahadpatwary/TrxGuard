package com.txnrelay.ui

import com.txnrelay.config.PaymentApp
import com.txnrelay.config.TargetApps
import com.txnrelay.data.model.EventStatus
import com.txnrelay.data.model.StatusCounts
import com.txnrelay.data.settings.AppSettings

/** UI-friendly view of one stored event. */
data class EventUi(
    val id: String,
    val app: PaymentApp?,
    val title: String,
    val body: String,
    val postedAt: Long,
    val status: EventStatus,
    val retryCount: Int,
    val lastError: String?,
)

data class MainUiState(
    val accessEnabled: Boolean = false,
    val monitoredApps: List<String> = TargetApps.displayNames,
    val counts: StatusCounts = StatusCounts(),
    val events: List<EventUi> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isSyncing: Boolean = false,
    val lastSyncError: String? = null,
)

/** All user actions the screen can trigger (keeps composables free of ViewModel references). */
class MainActions(
    val onOpenAccessSettings: () -> Unit,
    val onOpenAppInfo: () -> Unit,
    val onSyncNow: () -> Unit,
    val onAddTestEvent: () -> Unit,
    val onSaveConnection: (endpoint: String, apiKey: String) -> Unit,
    val onAutoSyncChange: (Boolean) -> Unit,
    val onThemeChange: (com.txnrelay.data.settings.ThemeMode) -> Unit,
)
