package com.jackiechen.quotadroid.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jackiechen.quotadroid.MainActivity
import com.jackiechen.quotadroid.R
import com.jackiechen.quotadroid.ui.labelRes
import kotlin.math.roundToInt

object Notifications {
    const val CHANNEL_ID = "quota_alerts"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_alerts_description)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun show(context: Context, alert: AlertPolicy.Alert) {
        if (!hasPermission(context)) return

        val windowLabel = context.getString(alert.window.key.labelRes)
        val title = context.getString(R.string.notification_title, windowLabel)
        val body = context.getString(
            R.string.notification_body,
            alert.window.usedPercent.roundToInt(),
            alert.threshold,
        )
        val intent = PendingIntent.getActivity(
            context,
            alert.window.key.ordinal,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quota)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ALERT_ID_BASE + alert.window.key.ordinal, notification)
    }

    private fun hasPermission(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private const val ALERT_ID_BASE = 100
}
