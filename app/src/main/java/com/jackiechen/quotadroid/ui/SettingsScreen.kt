package com.jackiechen.quotadroid.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jackiechen.quotadroid.R
import com.jackiechen.quotadroid.data.AppSettings
import com.jackiechen.quotadroid.data.QuotaLevel
import com.jackiechen.quotadroid.data.QuotaWindowKey
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: QuotaUiState,
    onBack: () -> Unit,
    onSave: (apiKey: String, settings: AppSettings, apiKeyDirty: Boolean) -> Unit,
    onTest: (apiKey: String, workspaceId: String) -> Unit,
    onClearKey: () -> Unit,
) {
    val initial = state.settings
    var apiKey by rememberSaveable { mutableStateOf("") }
    var workspaceId by rememberSaveable { mutableStateOf(initial.workspaceId) }
    var refreshMinutes by rememberSaveable { mutableStateOf(initial.refreshMinutes) }
    var thresholdRolling by rememberSaveable { mutableStateOf(initial.thresholdRolling) }
    var thresholdWeekly by rememberSaveable { mutableStateOf(initial.thresholdWeekly) }
    var thresholdMonthly by rememberSaveable { mutableStateOf(initial.thresholdMonthly) }
    var notifications by rememberSaveable { mutableStateOf(initial.notificationsEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_credentials),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_api_key)) },
                        placeholder = {
                            Text(
                                if (state.hasApiKey) stringResource(R.string.settings_api_key_set)
                                else stringResource(R.string.settings_api_key_hint),
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Text(
                        text = stringResource(R.string.settings_api_key_hint_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = workspaceId,
                        onValueChange = { workspaceId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_workspace)) },
                        placeholder = { Text(stringResource(R.string.settings_workspace_hint)) },
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onTest(apiKey, workspaceId) },
                            enabled = state.testState !is TestState.Running,
                        ) {
                            Text(stringResource(R.string.settings_test))
                        }
                        TextButton(onClick = onClearKey, enabled = state.hasApiKey) {
                            Text(stringResource(R.string.settings_clear_key))
                        }
                    }
                    when (val testState = state.testState) {
                        TestState.Idle -> Unit
                        TestState.Running -> Text(
                            text = stringResource(R.string.settings_testing),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TestState.Success -> Text(
                            text = stringResource(R.string.settings_test_ok),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        is TestState.Failure -> Text(
                            text = testFailureMessage(testState),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_refresh),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppSettings.REFRESH_OPTIONS.forEach { minutes ->
                            FilterChip(
                                selected = refreshMinutes == minutes,
                                onClick = { refreshMinutes = minutes },
                                label = { Text(stringResource(R.string.minutes_value, minutes)) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.settings_refresh_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_thresholds),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_thresholds_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ThresholdSlider(
                        label = stringResource(QuotaWindowKey.ROLLING_5H.labelRes),
                        value = thresholdRolling,
                        onChange = { thresholdRolling = it },
                    )
                    ThresholdSlider(
                        label = stringResource(QuotaWindowKey.WEEKLY.labelRes),
                        value = thresholdWeekly,
                        onChange = { thresholdWeekly = it },
                    )
                    ThresholdSlider(
                        label = stringResource(QuotaWindowKey.MONTHLY.labelRes),
                        value = thresholdMonthly,
                        onChange = { thresholdMonthly = it },
                    )
                }
            }

            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_notifications),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_notifications_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = notifications, onCheckedChange = { notifications = it })
                }
            }

            Button(
                onClick = {
                    onSave(
                        apiKey,
                        AppSettings(
                            workspaceId = workspaceId,
                            refreshMinutes = refreshMinutes,
                            thresholdRolling = thresholdRolling,
                            thresholdWeekly = thresholdWeekly,
                            thresholdMonthly = thresholdMonthly,
                            notificationsEnabled = notifications,
                        ),
                        apiKey.isNotBlank(),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_save))
            }

            Text(
                text = stringResource(R.string.privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ThresholdSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.percent_value, value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 50f..100f,
            steps = 9,
        )
    }
}

@Composable
private fun testFailureMessage(testState: TestState.Failure): String =
    when (val error = testState.error) {
        com.jackiechen.quotadroid.data.QuotaError.MissingApiKey ->
            stringResource(R.string.error_missing_key)
        com.jackiechen.quotadroid.data.QuotaError.Unauthorized ->
            stringResource(R.string.error_unauthorized)
        com.jackiechen.quotadroid.data.QuotaError.Forbidden ->
            stringResource(R.string.error_forbidden)
        com.jackiechen.quotadroid.data.QuotaError.Throttled ->
            stringResource(R.string.error_throttled)
        is com.jackiechen.quotadroid.data.QuotaError.Http ->
            stringResource(R.string.error_http, error.code)
        is com.jackiechen.quotadroid.data.QuotaError.Parse ->
            stringResource(R.string.error_parse, error.detail)
        is com.jackiechen.quotadroid.data.QuotaError.Network ->
            stringResource(R.string.error_network, error.detail)
        is com.jackiechen.quotadroid.data.QuotaError.Unknown ->
            stringResource(R.string.error_unknown, error.detail)
    }
