package com.excalibur.routines.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.excalibur.routines.MainActivity
import com.excalibur.routines.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveRoutineNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "active_routine_channel"
        const val NOTIFICATION_ID = 9002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Routine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and timer for ongoing routines"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOrUpdateNotification(
        routineName: String,
        stepName: String,
        stepIndex: Int,
        totalSteps: Int,
        formattedRemainingTime: String,
        isPaused: Boolean,
        isBonusLounge: Boolean
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBonusLounge) "🎉 Bonus Gap • $routineName" else "$routineName • $stepName"
        val statusPrefix = if (isPaused) "⏸️ PAUSED" else formattedRemainingTime
        val contentText = "$statusPrefix remaining • Step ${stepIndex + 1} of $totalSteps"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingOpenIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
