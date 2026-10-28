package com.jackiechen.quotadroid.data

import java.time.Duration
import java.time.Instant

/** The three allowance windows exposed by the OpenCode Go usage API. */
enum class QuotaWindowKey(val apiName: String) {
    ROLLING_5H("rolling"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    companion object {
        fun fromApiName(name: String): QuotaWindowKey? = entries.firstOrNull { it.apiName == name }
    }
}

enum class QuotaLevel { HEALTHY, WARNING, CRITICAL }

/**
 * One normalized quota window. The API only reports a percentage, so the model
 * keeps a 100-point scale: used = reported percent, remaining = 100 - used.
 */
data class QuotaWindow(
    val key: QuotaWindowKey,
    val usedPercent: Double,
    val resetsAt: Instant,
    val limited: Boolean,
) {
    val remainingPercent: Double
        get() = (100.0 - usedPercent).coerceAtLeast(0.0)

    val level: QuotaLevel
        get() = when {
            limited || usedPercent >= CRITICAL_PERCENT -> QuotaLevel.CRITICAL
            usedPercent >= WARNING_PERCENT -> QuotaLevel.WARNING
            else -> QuotaLevel.HEALTHY
        }

    companion object {
        const val WARNING_PERCENT = 70.0
        const val CRITICAL_PERCENT = 90.0
    }
}

/** A complete usage reading, as returned by one successful fetch. */
data class UsageSnapshot(
    val windows: List<QuotaWindow>,
    val fetchedAt: Instant,
) {
    val peakUsedPercent: Double
        get() = windows.maxOfOrNull { it.usedPercent } ?: 0.0

    val nextReset: QuotaWindow?
        get() = windows.minByOrNull { it.resetsAt }

    val peakLevel: QuotaLevel
        get() = when {
            windows.any { it.limited } || peakUsedPercent >= QuotaWindow.CRITICAL_PERCENT -> QuotaLevel.CRITICAL
            peakUsedPercent >= QuotaWindow.WARNING_PERCENT -> QuotaLevel.WARNING
            else -> QuotaLevel.HEALTHY
        }

    fun window(key: QuotaWindowKey): QuotaWindow? = windows.firstOrNull { it.key == key }
}

/** Whole seconds until [target], clamped at zero. */
fun secondsUntil(target: Instant, now: Instant = Instant.now()): Long =
    Duration.between(now, target).seconds.coerceAtLeast(0L)
