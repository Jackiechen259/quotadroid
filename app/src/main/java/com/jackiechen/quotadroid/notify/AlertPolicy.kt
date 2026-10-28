package com.jackiechen.quotadroid.notify

import com.jackiechen.quotadroid.data.AppSettings
import com.jackiechen.quotadroid.data.QuotaWindow
import com.jackiechen.quotadroid.data.SettingsStore
import com.jackiechen.quotadroid.data.UsageSnapshot

/** Decides which threshold breaches still owe the user a notification. */
object AlertPolicy {
    data class Alert(val window: QuotaWindow, val threshold: Int)

    /**
     * Returns the windows that just crossed their threshold and marks their
     * reset cycle as notified, so one reset produces at most one alert.
     */
    suspend fun collectPending(
        snapshot: UsageSnapshot,
        settings: AppSettings,
        store: SettingsStore,
    ): List<Alert> {
        val pending = mutableListOf<Alert>()
        for (window in snapshot.windows) {
            val threshold = settings.thresholdFor(window.key)
            val breached = window.limited || window.usedPercent >= threshold
            if (!breached) continue

            val cycle = window.resetsAt.toEpochMilli()
            if (store.alertCycle(window.key) == cycle) continue

            store.markAlerted(window.key, cycle)
            pending += Alert(window, threshold)
        }
        return pending
    }
}
