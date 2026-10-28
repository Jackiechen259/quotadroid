package com.jackiechen.quotadroid.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageParserTest {
    private val now = Instant.parse("2026-10-27T04:00:00Z")

    private val payload = """
        {
          "usage": {
            "rolling":  { "status": "ok",           "percent": 78,   "resetsAt": "2026-10-27T05:31:52Z" },
            "weekly":   { "status": "ok",           "percent": 52.5, "resetsAt": "2026-10-31T00:00:00Z" },
            "monthly":  { "status": "rate-limited", "percent": 100,  "resetsAt": "2026-11-01T00:00:00Z" }
          }
        }
    """.trimIndent()

    @Test
    fun `parses all three windows`() {
        val snapshot = UsageParser.parse(payload, now)

        assertEquals(3, snapshot.windows.size)
        assertEquals(listOf(QuotaWindowKey.ROLLING_5H, QuotaWindowKey.WEEKLY, QuotaWindowKey.MONTHLY), snapshot.windows.map { it.key })
        assertEquals(now, snapshot.fetchedAt)
        assertEquals(78.0, snapshot.window(QuotaWindowKey.ROLLING_5H)!!.usedPercent, 0.001)
        assertEquals(22.0, snapshot.window(QuotaWindowKey.ROLLING_5H)!!.remainingPercent, 0.001)
        assertEquals(Instant.parse("2026-10-31T00:00:00Z"), snapshot.window(QuotaWindowKey.WEEKLY)!!.resetsAt)
    }

    @Test
    fun `treats rate-limited as an exhausted window`() {
        val monthly = UsageParser.parse(payload, now).window(QuotaWindowKey.MONTHLY)!!

        assertTrue(monthly.limited)
        assertEquals(QuotaLevel.CRITICAL, monthly.level)
        assertEquals(0.0, monthly.remainingPercent, 0.001)
    }

    @Test
    fun `skips windows the API omits`() {
        val snapshot = UsageParser.parse(
            """{"usage":{"weekly":{"status":"ok","percent":10,"resetsAt":"2026-10-31T00:00:00Z"}}}""",
            now,
        )

        assertEquals(listOf(QuotaWindowKey.WEEKLY), snapshot.windows.map { it.key })
    }

    @Test
    fun `ignores unknown fields and casing of status`() {
        val snapshot = UsageParser.parse(
            """{"usage":{"rolling":{"status":"OK","percent":10,"resetsAt":"2026-10-27T05:31:52Z","future":1}},"extra":true}""",
            now,
        )

        assertFalse(snapshot.window(QuotaWindowKey.ROLLING_5H)!!.limited)
    }

    @Test
    fun `clamps percentages into the 100 point scale`() {
        val window = UsageParser.parse(
            """{"usage":{"rolling":{"status":"ok","percent":140,"resetsAt":"2026-10-27T05:31:52Z"}}}""",
            now,
        ).window(QuotaWindowKey.ROLLING_5H)!!

        assertEquals(100.0, window.usedPercent, 0.001)
        assertEquals(0.0, window.remainingPercent, 0.001)
    }

    @Test
    fun `fails when no window can be read`() {
        assertThrows(QuotaError.Parse::class.java) {
            UsageParser.parse("""{"usage":{}}""", now)
        }
    }

    @Test
    fun `fails on an invalid reset timestamp`() {
        assertThrows(QuotaError.Parse::class.java) {
            UsageParser.parse(
                """{"usage":{"rolling":{"status":"ok","percent":10,"resetsAt":"tomorrow"}}}""",
                now,
            )
        }
    }

    @Test
    fun `fails on malformed json instead of reporting zero usage`() {
        assertThrows(QuotaError.Parse::class.java) {
            UsageParser.parse("<html>gateway error</html>", now)
        }
    }
}
