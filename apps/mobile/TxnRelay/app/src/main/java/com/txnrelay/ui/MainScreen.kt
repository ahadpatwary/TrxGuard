package com.txnrelay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txnrelay.ui.components.EmptyEvents
import com.txnrelay.ui.components.EventRow
import com.txnrelay.ui.components.PermissionCard
import com.txnrelay.ui.components.QueueCard
import com.txnrelay.ui.components.SettingsCard
import com.txnrelay.ui.theme.TxnRelayTheme

/** Glue between ViewModel and the stateless [MainScreen]. */
@Composable
fun MainRoute(
    viewModel: MainViewModel,
    onOpenAccessSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val actions = remember(viewModel) {
        MainActions(
            onOpenAccessSettings = onOpenAccessSettings,
            onOpenAppInfo = onOpenAppInfo,
            onSyncNow = viewModel::syncNow,
            onAddTestEvent = viewModel::addTestEvent,
            onSaveConnection = viewModel::saveConnection,
            onAutoSyncChange = viewModel::setAutoSync,
            onThemeChange = viewModel::setTheme,
        )
    }

    TxnRelayTheme(themeMode = state.settings.themeMode) {
        MainScreen(state = state, actions = actions, snackbarHostState = snackbarHostState)
    }
}

@Composable
fun MainScreen(
    state: MainUiState,
    actions: MainActions,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = inner.calculateTopPadding() + 12.dp,
                bottom = inner.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header() }

            item {
                PermissionCard(
                    enabled = state.accessEnabled,
                    monitoredApps = state.monitoredApps,
                    onOpenSettings = actions.onOpenAccessSettings,
                    onOpenAppInfo = actions.onOpenAppInfo,
                )
            }

            item {
                QueueCard(
                    counts = state.counts,
                    isSyncing = state.isSyncing,
                    lastError = state.lastSyncError,
                    onSyncNow = actions.onSyncNow,
                    onAddTestEvent = actions.onAddTestEvent,
                )
            }

            item {
                SettingsCard(
                    settings = state.settings,
                    onSaveConnection = actions.onSaveConnection,
                    onAutoSyncChange = actions.onAutoSyncChange,
                    onThemeChange = actions.onThemeChange,
                )
            }

            item {
                Text(
                    text = "Recent events",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (state.events.isEmpty()) {
                item { EmptyEvents() }
            } else {
                items(state.events, key = { it.id }) { event -> EventRow(event) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "TxnRelay",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "bKash & Nagad notification relay",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
