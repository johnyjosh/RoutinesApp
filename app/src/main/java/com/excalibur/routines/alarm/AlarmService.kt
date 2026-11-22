package com.excalibur.routines.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.excalibur.routines.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        const val ACTION_START_ALARM = "com.excalibur.routines.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.excalibur.routines.ACTION_STOP_ALARM"

        const val CHANNEL_ID = "routines_alarms_channel"
        const val NOTIFICATION_ID = 9001
    }

    @Inject
    lateinit var alarmSoundManager: AlarmSoundManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        Log.d(TAG, "onStartCommand action: $action")

        if (action == ACTION_STOP_ALARM) {
            stopAlarmAndService()
            return START_NOT_STICKY
        }

        val routineName = intent?.getStringExtra(AlarmSchedulerImpl.EXTRA_ROUTINE_NAME) ?: "Routine Alarm"
        val stepName = intent?.getStringExtra(AlarmSchedulerImpl.EXTRA_STEP_NAME) ?: "Step"
        val stepIndex = intent?.getIntExtra(AlarmSchedulerImpl.EXTRA_STEP_INDEX, 0) ?: 0
        val totalSteps = intent?.getIntExtra(AlarmSchedulerImpl.EXTRA_TOTAL_STEPS, 1) ?: 1
        val nextStepName = intent?.getStringExtra(AlarmSchedulerImpl.EXTRA_NEXT_STEP_NAME)
        val soundEnabled = intent?.getBooleanExtra(AlarmSchedulerImpl.EXTRA_SOUND_ENABLED, true) ?: true
        val vibrateEnabled = intent?.getBooleanExtra(AlarmSchedulerImpl.EXTRA_VIBRATE_ENABLED, true) ?: true

        val notification = buildNotification(
            routineName = routineName,
            stepName = stepName,
            stepIndex = stepIndex,
            totalSteps = totalSteps,
            nextStepName = nextStepName,
            originalIntent = intent
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        alarmSoundManager.startAlarm(soundEnabled, vibrateEnabled)

        return START_NOT_STICKY
    }

    private fun stopAlarmAndService() {
        alarmSoundManager.stopAlarm()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmSoundManager.stopAlarm()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Routine Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications and full screen alerts for active routine steps"
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        routineName: String,
        stepName: String,
        stepIndex: Int,
        totalSteps: Int,
        nextStepName: String?,
        originalIntent: Intent?
    ): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            originalIntent?.let { putExtras(it) }
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "$stepName • Step ${stepIndex + 1} of $totalSteps"
        val subText = nextStepName?.let { "Next: $it" }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(routineName)
            .setContentText(contentText)
            .setSubText(subText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()
    }
}
