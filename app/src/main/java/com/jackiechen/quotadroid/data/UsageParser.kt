package com.jackiechen.quotadroid.data

import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses `GET https://opencode.ai/zen/go/v1/usage` into the shared quota model.
 *
 * The payload is one meter per window:
 *
 * ```json
 * { "usage": { "rolling":  { "status": "ok",           "percent": 78.0,  "resetsAt": "2026-10-27T05:31:52Z" },
 *              "weekly":   { "status": "ok",           "percent": 52.0,  "resetsAt": "2026-10-31T00:00:00Z" },
 *              "monthly":  { "status": "rate-limited", "percent": 100.0, "resetsAt": "2026-11-01T00:00:00Z" } } }
 * ```
 *
 * Missing windows are skipped, unknown fields are ignored, and anything that
 * cannot be understood raises [QuotaError.Parse].
 */
object UsageParser {
    private const val RATE_LIMITED = "rate-limited"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(payload: String, now: Instant = Instant.now()): UsageSnapshot {
        val envelope = try {
            json.decodeFromString(UsageEnvelope.serializer(), payload)
        } catch (error: Exception) {
            throw QuotaError.Parse("invalid JSON: ${error.message}")
        }

        val windows = buildList {
            envelope.usage.rolling?.let { add(it.toWindow(QuotaWindowKey.ROLLING_5H)) }
            envelope.usage.weekly?.let { add(it.toWindow(QuotaWindowKey.WEEKLY)) }
            envelope.usage.monthly?.let { add(it.toWindow(QuotaWindowKey.MONTHLY)) }
        }
        if (windows.isEmpty()) {
            throw QuotaError.Parse("the response contains no quota windows")
        }
        return UsageSnapshot(windows = windows, fetchedAt = now)
    }

    private fun Meter.toWindow(key: QuotaWindowKey): QuotaWindow {
        if (!percent.isFinite()) {
            throw QuotaError.Parse("the ${key.apiName} window has a non-finite percentage")
        }
        val reset = try {
            Instant.parse(resetsAt.trim())
        } catch (error: DateTimeParseException) {
            throw QuotaError.Parse("invalid resetsAt for ${key.apiName}: ${error.message}")
        }
        return QuotaWindow(
            key = key,
            usedPercent = percent.coerceIn(0.0, 100.0),
            resetsAt = reset,
            limited = status.trim().equals(RATE_LIMITED, ignoreCase = true),
        )
    }
}

@Serializable
private data class UsageEnvelope(val usage: UsageMeters = UsageMeters())

@Serializable
private data class UsageMeters(
    val rolling: Meter? = null,
    val weekly: Meter? = null,
    val monthly: Meter? = null,
)

@Serializable
private data class Meter(
    val status: String = "ok",
    val percent: Double = 0.0,
    @SerialName("resetsAt") val resetsAt: String = "",
)
