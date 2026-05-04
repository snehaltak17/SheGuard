package com.sheguard.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sheguard.R

class SafetyNotificationManager(private val context: Context) {

    fun showDangerZone(zoneName: String?) {
        showNotification(
            notificationId = 2001,
            title = context.getString(R.string.danger_zone_detected),
            message = if (zoneName.isNullOrBlank()) {
                context.getString(R.string.danger_zone_message_generic)
            } else {
                context.getString(R.string.danger_zone_message_named, zoneName)
            }
        )
    }

    fun showSafeZone(zoneName: String?) {
        showNotification(
            notificationId = 2002,
            title = context.getString(R.string.safe_zone_detected),
            message = if (zoneName.isNullOrBlank()) {
                context.getString(R.string.safe_zone_message_generic)
            } else {
                context.getString(R.string.safe_zone_message_named, zoneName)
            }
        )
    }

    fun showMovedOutOfDanger() {
        showNotification(
            notificationId = 2003,
            title = context.getString(R.string.left_danger_zone_title),
            message = context.getString(R.string.left_danger_zone_message)
        )
    }

    private fun showNotification(notificationId: Int, title: String, message: String) {
        createChannelIfNeeded()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.zone_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.zone_alert_channel_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "sheguard_zone_alerts"
    }
}
