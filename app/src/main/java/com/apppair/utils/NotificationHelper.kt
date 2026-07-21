package com.apppair.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.apppair.R
import com.apppair.service.AppPairForegroundService
import com.apppair.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "apppair_service_channel"
    const val NOTIFICATION_ID = 1001

    const val ACTION_STOP_SERVICE = "com.apppair.action.STOP_SERVICE"
    const val ACTION_RESET_WIDGET_POSITION = "com.apppair.action.RESET_WIDGET_POSITION"
    const val ACTION_OPEN_SETTINGS = "com.apppair.action.OPEN_SETTINGS"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildForegroundNotification(
        context: Context,
        labelA: String,
        labelB: String,
        warningMessage: String? = null
    ): Notification {
        createNotificationChannel(context)

        val title = if (warningMessage != null) {
            "AppPair Alert: $warningMessage"
        } else {
            "AppPair Active: $labelA ⇄ $labelB"
        }

        val content = if (warningMessage != null) {
            "Tap to open settings and resolve app status."
        } else {
            "Persistent switcher running. Both apps monitored against system kill."
        }

        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop service action
        val stopIntent = Intent(context, AppPairForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reset widget position action
        val resetIntent = Intent(context, AppPairForegroundService::class.java).apply {
            action = ACTION_RESET_WIDGET_POSITION
        }
        val resetPendingIntent = PendingIntent.getService(
            context,
            2,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Reset Widget", resetPendingIntent)
            .build()
    }
}
