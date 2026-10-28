package com.jackiechen.quotadroid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jackiechen.quotadroid.data.AppSettings
import com.jackiechen.quotadroid.data.OpenCodeClient
import com.jackiechen.quotadroid.data.QuotaError
import com.jackiechen.quotadroid.data.QuotaRepository
import com.jackiechen.quotadroid.data.SettingsStore
import com.jackiechen.quotadroid.data.UsageParser
import com.jackiechen.quotadroid.data.UsageSnapshot
import com.jackiechen.quotadroid.work.QuotaScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TestState {
    data object Idle : TestState
    data object Running : TestState
    data object Success : TestState
    data class Failure(val error: QuotaError) : TestState
}

data class QuotaUiState(
    val settings: AppSettings = AppSettings(),
    val settingsLoaded: Boolean = false,
    val hasApiKey: Boolean = false,
    val snapshot: UsageSnapshot? = null,
    val refreshing: Boolean = false,
    val error: QuotaError? = null,
    val testState: TestState = TestState.Idle,
)

class QuotaViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val repository = QuotaRepository(OpenCodeClient(), store)

    private val _ui = MutableStateFlow(QuotaUiState())
    val ui: StateFlow<QuotaUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            store.settings.collect { settings ->
                _ui.update { it.copy(settings = settings, settingsLoaded = true) }
            }
        }
        viewModelScope.launch {
            store.hasApiKey.collect { hasKey -> _ui.update { it.copy(hasApiKey = hasKey) } }
        }
        viewModelScope.launch {
            val cached = repository.cached()
            if (cached != null) _ui.update { it.copy(snapshot = cached) }
            refresh(silent = cached != null)
        }
    }

    fun refresh(silent: Boolean = false) {
        if (_ui.value.refreshing) return
        viewModelScope.launch {
            _ui.update { current -> current.copy(refreshing = true, error = if (silent) current.error else null) }
            val result = repository.refresh()
            _ui.update { current ->
                result.fold(
                    onSuccess = { snapshot ->
                        current.copy(snapshot = snapshot, refreshing = false, error = null)
                    },
                    onFailure = { error ->
                        current.copy(
                            refreshing = false,
                            error = error as? QuotaError ?: QuotaError.Unknown(error.message.orEmpty()),
                        )
                    },
                )
            }
        }
    }

    fun saveSettings(apiKey: String, settings: AppSettings, apiKeyDirty: Boolean) {
        viewModelScope.launch {
            if (apiKeyDirty) store.saveApiKey(apiKey)
            val sanitized = settings.copy(
                workspaceId = settings.workspaceId.trim(),
                refreshMinutes = settings.refreshMinutes.coerceIn(AppSettings.MIN_REFRESH_MINUTES, 24 * 60),
            )
            store.update { sanitized }
            val app = getApplication<Application>()
            if (sanitized.notificationsEnabled) {
                QuotaScheduler.schedule(app, sanitized.refreshMinutes)
            } else {
                QuotaScheduler.cancel(app)
            }
            _ui.update { it.copy(testState = TestState.Idle) }
            refresh()
        }
    }

    fun testConnection(apiKey: String, workspaceId: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) {
                _ui.update { it.copy(testState = TestState.Failure(QuotaError.MissingApiKey)) }
                return@launch
            }
            _ui.update { it.copy(testState = TestState.Running) }
            val result = runCatching {
                UsageParser.parse(OpenCodeClient().fetchUsage(apiKey, workspaceId))
            }
            _ui.update { current ->
                current.copy(
                    testState = result.fold(
                        onSuccess = { TestState.Success },
                        onFailure = { error ->
                            TestState.Failure(error as? QuotaError ?: QuotaError.Unknown(error.message.orEmpty()))
                        },
                    ),
                )
            }
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            store.saveApiKey("")
            store.clearSnapshot()
            _ui.update { it.copy(testState = TestState.Idle) }
        }
    }
}
