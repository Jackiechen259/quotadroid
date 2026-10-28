package com.jackiechen.quotadroid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jackiechen.quotadroid.R
import com.jackiechen.quotadroid.data.QuotaError
import com.jackiechen.quotadroid.data.QuotaLevel
import com.jackiechen.quotadroid.data.QuotaWindow
import com.jackiechen.quotadroid.data.UsageSnapshot
import com.jackiechen.quotadroid.data.secondsUntil
import com.jackiechen.quotadroid.ui.theme.LevelCritical
import com.jackiechen.quotadroid.ui.theme.LevelHealthy
import com.jackiechen.quotadroid.ui.theme.LevelWarning
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: QuotaUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1_000L)
        }
    }

    val refreshMinutes = state.settings.refreshMinutes
    LaunchedEffect(refreshMinutes) {
        while (true) {
            delay(refreshMinutes.coerceAtLeast(1) * 60_000L)
            onRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.refreshing) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val snapshot = state.snapshot
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !state.hasApiKey -> SetupCard(onOpenSettings = onOpenSettings)
                snapshot == null && state.refreshing -> LoadingCard()
                snapshot == null -> ErrorCard(message = errorMessage(state.error), onRetry = onRefresh)
                else -> {
                    state.error?.let { error ->
                        ErrorBanner(message = errorMessage(error), onRetry = onRefresh)
                    }
                    HeroCard(snapshot = snapshot, refreshing = state.refreshing, now = now)
                    snapshot.windows.forEach { window -> WindowCard(window = window, now = now) }
                    Footer(snapshot = snapshot, now = now)
                }
            }
        }
    }
}

@Composable
private fun SetupCard(onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.empty_action))
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.loading_usage), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun HeroCard(snapshot: UsageSnapshot, refreshing: Boolean, now: Instant) {
    val peakLevel = snapshot.peakLevel
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.hero_peak),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.percent_value, snapshot.peakUsedPercent.roundToInt()),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                StatusChip(level = peakLevel)
            }

            Spacer(Modifier.height(12.dp))
            snapshot.nextReset?.let { window ->
                Text(
                    text = stringResource(
                        R.string.hero_next_reset_hint,
                        stringResource(window.key.labelRes),
                        formatCountdown(window.resetsAt, now),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun WindowCard(window: QuotaWindow, now: Instant) {
    val accent = levelColor(window.level)
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(window.key.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.window_used, window.usedPercent.roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(level = window.level)
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (window.usedPercent / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.16f),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.window_remaining, window.remainingPercent.roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(R.string.window_resets_in, formatCountdown(window.resetsAt, now)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(level: QuotaLevel) {
    val accent = levelColor(level)
    Surface(
        color = accent.copy(alpha = 0.16f),
        contentColor = accent,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = stringResource(level.labelRes),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun Footer(snapshot: UsageSnapshot, now: Instant) {
    val formatter = remember { DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.getDefault()) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(
                R.string.last_updated,
                formatter.format(snapshot.fetchedAt.atZone(ZoneId.systemDefault())),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.subtitle_source),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun formatCountdown(target: Instant, now: Instant): String {
    val total = secondsUntil(target, now)
    val days = total / 86_400
    val hours = (total % 86_400) / 3_600
    val minutes = (total % 3_600) / 60
    return when {
        days > 0 -> stringResource(R.string.countdown_days_hours, days.toInt(), hours.toInt())
        hours > 0 -> stringResource(R.string.countdown_hours_minutes, hours.toInt(), minutes.toInt())
        minutes > 0 -> stringResource(R.string.countdown_minutes, minutes.toInt())
        else -> stringResource(R.string.countdown_seconds, total.toInt())
    }
}

@Composable
private fun errorMessage(error: QuotaError?): String = when (error) {
    null -> stringResource(R.string.error_generic)
    QuotaError.MissingApiKey -> stringResource(R.string.error_missing_key)
    QuotaError.Unauthorized -> stringResource(R.string.error_unauthorized)
    QuotaError.Forbidden -> stringResource(R.string.error_forbidden)
    QuotaError.Throttled -> stringResource(R.string.error_throttled)
    is QuotaError.Http -> stringResource(R.string.error_http, error.code)
    is QuotaError.Parse -> stringResource(R.string.error_parse, error.detail)
    is QuotaError.Network -> stringResource(R.string.error_network, error.detail)
    is QuotaError.Unknown -> stringResource(R.string.error_unknown, error.detail)
}

private fun levelColor(level: QuotaLevel): Color = when (level) {
    QuotaLevel.HEALTHY -> LevelHealthy
    QuotaLevel.WARNING -> LevelWarning
    QuotaLevel.CRITICAL -> LevelCritical
}
