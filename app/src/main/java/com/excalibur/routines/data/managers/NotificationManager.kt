package com.excalibur.routines.data.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.excalibur.routines.R
import com.excalibur.routines.services.AlarmService

class AppNotificationManager(private val context: Context) {

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel_id"
        const val ALARM_NOTIFICATION_ID = 123
        private const val CHANNEL_NAME = "Alarm Channel"
        private const val CHANNEL_DESCRIPTION = "Channel for Alarm manager"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createAlarmNotification(alarmTime: String, alarmTitle: String): android.app.Notification {
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setContentTitle("Alarm Ringing!")
            .setContentText("$alarmTitle - Time: $alarmTime")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    fun showNotification(notification: android.app.Notification, notificationId: Int = ALARM_NOTIFICATION_ID) {
        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int = ALARM_NOTIFICATION_ID) {
        notificationManager.cancel(notificationId)
    }
}