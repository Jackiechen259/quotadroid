package com.jackiechen.quotadroid.ui

import androidx.annotation.StringRes
import com.jackiechen.quotadroid.R
import com.jackiechen.quotadroid.data.QuotaLevel
import com.jackiechen.quotadroid.data.QuotaWindowKey

val QuotaWindowKey.labelRes: Int
    @StringRes get() = when (this) {
        QuotaWindowKey.ROLLING_5H -> R.string.window_rolling
        QuotaWindowKey.WEEKLY -> R.string.window_weekly
        QuotaWindowKey.MONTHLY -> R.string.window_monthly
    }

val QuotaLevel.labelRes: Int
    @StringRes get() = when (this) {
        QuotaLevel.HEALTHY -> R.string.status_healthy
        QuotaLevel.WARNING -> R.string.status_warning
        QuotaLevel.CRITICAL -> R.string.status_critical
    }
