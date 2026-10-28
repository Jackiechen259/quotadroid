package com.jackiechen.quotadroid.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jackiechen.quotadroid.data.AppSettings
import com.jackiechen.quotadroid.data.OpenCodeClient
import com.jackiechen.quotadroid.data.QuotaError
import com.jackiechen.quotadroid.data.QuotaRepository
import com.jackiechen.quotadroid.data.SettingsStore
import com.jackiechen.quotadroid.notify.AlertPolicy
import com.jackiechen.quotadroid.notify.Notifications
import java.util.concurrent.TimeUnit

/** Refreshes in the background and raises at most one alert per window/reset cycle. */
class QuotaWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val settings = store.currentSettings()
        if (!settings.notificationsEnabled) return Result.success()

        val repository = QuotaRepository(OpenCodeClient(), store)
        val snapshot = repository.refresh().getOrElse { error ->
            return if (error is QuotaError.Network) Result.retry() else Result.success()
        }
        AlertPolicy.collectPending(snapshot, settings, store).forEach {
            Notifications.show(applicationContext, it)
        }
        return Result.success()
    }
}

object QuotaScheduler {
    private const val WORK_NAME = "quota-refresh"

    fun schedule(context: Context, minutes: Int) {
        val interval = minutes.coerceAtLeast(AppSettings.MIN_REFRESH_MINUTES).toLong()
        val request = PeriodicWorkRequestBuilder<QuotaWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
