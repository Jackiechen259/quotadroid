package com.jackiechen.quotadroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun QuotaApp(viewModel: QuotaViewModel = viewModel()) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            state = state,
            onBack = { showSettings = false },
            onSave = { apiKey, settings, dirty ->
                viewModel.saveSettings(apiKey, settings, dirty)
                showSettings = false
            },
            onTest = viewModel::testConnection,
            onClearKey = viewModel::clearApiKey,
        )
    } else {
        DashboardScreen(
            state = state,
            onRefresh = { viewModel.refresh() },
            onOpenSettings = { showSettings = true },
        )
    }
}
