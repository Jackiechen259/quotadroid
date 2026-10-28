package com.jackiechen.quotadroid

import android.app.Application
import com.jackiechen.quotadroid.data.SettingsStore
import com.jackiechen.quotadroid.notify.Notifications
import com.jackiechen.quotadroid.work.QuotaScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuotaApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)

        val store = SettingsStore(this)
        scope.launch {
            val settings = store.currentSettings()
            if (settings.notificationsEnabled) {
                QuotaScheduler.schedule(this@QuotaApplication, settings.refreshMinutes)
            }
        }
    }
}
