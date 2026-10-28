package com.jackiechen.quotadroid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "quotadroid")

data class AppSettings(
    val workspaceId: String = "",
    val refreshMinutes: Int = DEFAULT_REFRESH_MINUTES,
    val thresholdRolling: Int = DEFAULT_THRESHOLD,
    val thresholdWeekly: Int = DEFAULT_THRESHOLD,
    val thresholdMonthly: Int = DEFAULT_THRESHOLD,
    val notificationsEnabled: Boolean = true,
) {
    fun thresholdFor(key: QuotaWindowKey): Int = when (key) {
        QuotaWindowKey.ROLLING_5H -> thresholdRolling
        QuotaWindowKey.WEEKLY -> thresholdWeekly
        QuotaWindowKey.MONTHLY -> thresholdMonthly
    }

    companion object {
        const val DEFAULT_REFRESH_MINUTES = 15
        const val DEFAULT_THRESHOLD = 80
        const val MIN_REFRESH_MINUTES = 15
        val REFRESH_OPTIONS = listOf(15, 30, 60, 120, 240)
    }
}

/** Persists settings, the encrypted API key, and the last successful snapshot. */
class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val secureStore = SecureStore()
    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<AppSettings> = appContext.dataStore.data.map { prefs ->
        AppSettings(
            workspaceId = prefs[Keys.WORKSPACE_ID].orEmpty(),
            refreshMinutes = prefs[Keys.REFRESH_MINUTES] ?: AppSettings.DEFAULT_REFRESH_MINUTES,
            thresholdRolling = prefs[Keys.THRESHOLD_ROLLING] ?: AppSettings.DEFAULT_THRESHOLD,
            thresholdWeekly = prefs[Keys.THRESHOLD_WEEKLY] ?: AppSettings.DEFAULT_THRESHOLD,
            thresholdMonthly = prefs[Keys.THRESHOLD_MONTHLY] ?: AppSettings.DEFAULT_THRESHOLD,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
        )
    }

    val hasApiKey: Flow<Boolean> = appContext.dataStore.data.map { !it[Keys.API_KEY].isNullOrEmpty() }

    suspend fun currentSettings(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(currentSettings())
        appContext.dataStore.edit { prefs ->
            prefs[Keys.WORKSPACE_ID] = updated.workspaceId
            prefs[Keys.REFRESH_MINUTES] = updated.refreshMinutes
            prefs[Keys.THRESHOLD_ROLLING] = updated.thresholdRolling
            prefs[Keys.THRESHOLD_WEEKLY] = updated.thresholdWeekly
            prefs[Keys.THRESHOLD_MONTHLY] = updated.thresholdMonthly
            prefs[Keys.NOTIFICATIONS_ENABLED] = updated.notificationsEnabled
        }
    }

    suspend fun apiKey(): String {
        val stored = appContext.dataStore.data.first()[Keys.API_KEY].orEmpty()
        return secureStore.decrypt(stored).orEmpty()
    }

    suspend fun saveApiKey(value: String) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = secureStore.encrypt(value.trim())
        }
    }

    suspend fun saveSnapshot(snapshot: UsageSnapshot) {
        val cached = CachedSnapshot(
            windows = snapshot.windows.map {
                CachedWindow(it.key.apiName, it.usedPercent, it.resetsAt.toEpochMilli(), it.limited)
            },
            fetchedAt = snapshot.fetchedAt.toEpochMilli(),
        )
        appContext.dataStore.edit { prefs ->
            prefs[Keys.SNAPSHOT] = json.encodeToString(CachedSnapshot.serializer(), cached)
        }
    }

    suspend fun loadSnapshot(): UsageSnapshot? {
        val raw = appContext.dataStore.data.first()[Keys.SNAPSHOT] ?: return null
        return runCatching {
            val cached = json.decodeFromString(CachedSnapshot.serializer(), raw)
            UsageSnapshot(
                windows = cached.windows.mapNotNull { window ->
                    QuotaWindowKey.fromApiName(window.key)?.let { key ->
                        QuotaWindow(key, window.used, Instant.ofEpochMilli(window.resetAt), window.limited)
                    }
                },
                fetchedAt = Instant.ofEpochMilli(cached.fetchedAt),
            )
        }.getOrNull()?.takeIf { it.windows.isNotEmpty() }
    }

    suspend fun clearSnapshot() {
        appContext.dataStore.edit { prefs -> prefs.remove(Keys.SNAPSHOT) }
    }

    suspend fun alertCycle(key: QuotaWindowKey): Long? {
        val stored = appContext.dataStore.data.first()[Keys.alerted(key)]
        return stored?.takeIf { it > 0L }
    }

    suspend fun markAlerted(key: QuotaWindowKey, cycle: Long) {
        appContext.dataStore.edit { prefs -> prefs[Keys.alerted(key)] = cycle }
    }

    private object Keys {
        val WORKSPACE_ID = stringPreferencesKey("workspace_id")
        val API_KEY = stringPreferencesKey("api_key_encrypted")
        val REFRESH_MINUTES = intPreferencesKey("refresh_minutes")
        val THRESHOLD_ROLLING = intPreferencesKey("threshold_rolling")
        val THRESHOLD_WEEKLY = intPreferencesKey("threshold_weekly")
        val THRESHOLD_MONTHLY = intPreferencesKey("threshold_monthly")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SNAPSHOT = stringPreferencesKey("last_snapshot")

        fun alerted(key: QuotaWindowKey) = longPreferencesKey("alerted_cycle_${key.apiName}")
    }
}

@Serializable
internal data class CachedSnapshot(
    val windows: List<CachedWindow> = emptyList(),
    val fetchedAt: Long = 0L,
)

@Serializable
internal data class CachedWindow(
    val key: String,
    val used: Double,
    val resetAt: Long,
    val limited: Boolean,
)
