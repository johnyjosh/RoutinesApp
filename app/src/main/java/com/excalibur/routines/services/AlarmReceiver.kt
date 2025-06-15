package com.excalibur.routines.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.excalibur.routines.domain.services.RoutineAlarmScheduler
import com.excalibur.routines.data.repositories.AndroidAlarmRepository
import com.excalibur.routines.data.repositories.AndroidRoutineInstanceRepository
import com.excalibur.routines.data.repositories.AndroidRoutineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        private const val ALARM_CHANNEL_ID = "alarm_channel"
        private const val ALARM_NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 ALARM RECEIVED! Intent action: ${intent.action}")

        val alarmId = intent.getStringExtra("ALARM_ID") ?: "unknown"
        val alarmTime = intent.getStringExtra("ALARM_TIME") ?: "Alarm"
        val alarmTitle = intent.getStringExtra("ALARM_TITLE") ?: "Routine Time"
        val alarmDescription = intent.getStringExtra("ALARM_DESCRIPTION") ?: ""

        Log.d(TAG, "Alarm details - ID: $alarmId, Time: $alarmTime, Title: $alarmTitle")

        // Check if this is a routine alarm
        val routineAlarmScheduler = RoutineAlarmScheduler(null) // We only need parsing functionality
        val routineAlarmInfo = routineAlarmScheduler.parseRoutineAlarmId(alarmId)
        
        // Create notification channel for alarms
        createNotificationChannel(context)
        
        // Create intent for AlarmActivity
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TIME", alarmTime)
            putExtra("ALARM_TITLE", alarmTitle)
            putExtra("ALARM_DESCRIPTION", alarmDescription)
            
            // Add routine-specific information if this is a routine alarm
            routineAlarmInfo?.let { info ->
                putExtra("IS_ROUTINE_ALARM", true)
                putExtra("ROUTINE_INSTANCE_ID", info.routineInstanceId)
                putExtra("ROUTINE_STEP_INDEX", info.stepIndex)
                putExtra("ROUTINE_DAY_OF_WEEK", info.dayOfWeek.name)
                
                Log.d(TAG, "🏃 Routine alarm - Instance: ${info.routineInstanceId}, Step: ${info.stepIndex}, Day: ${info.dayOfWeek}")
            }
            
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Create PendingIntent for full-screen intent
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode(),
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Create dismiss intent to stop alarm when notification is dismissed
        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode() + 1000,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            Log.d(TAG, "Creating full-screen alarm notification")
            
            // Start alarm sound and vibration immediately using singleton manager
            AlarmSoundManager.startAlarm(context)
            
            // Get alarm sound URI for notification (backup)
            val alarmSoundUri = getAlarmSoundUri()
            
            // Create a full-screen notification that will launch the alarm activity
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
                .setContentTitle(alarmTitle)
                .setContentText(alarmDescription.ifEmpty { alarmTime })
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)  // This is the key for bypassing BAL restrictions
                .setContentIntent(pendingIntent)  // Also set as content intent
                .setDeleteIntent(dismissPendingIntent)  // Handle notification dismissal
                .setSound(alarmSoundUri)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()
            
            notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
            Log.d(TAG, "Full-screen alarm notification created with sound and vibration")
            
            // Handle automatic rescheduling for repeat alarms
            routineAlarmInfo?.let { info ->
                Log.d(TAG, "Checking if alarm needs rescheduling for routine ${info.routineInstanceId}")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "Starting rescheduling coroutine")
                        val sharedPreferences = context.getSharedPreferences("routines_prefs", Context.MODE_PRIVATE)
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        
                        val routineInstanceRepository = AndroidRoutineInstanceRepository(sharedPreferences)
                        val alarmRepository = AndroidAlarmRepository(context, alarmManager, sharedPreferences)
                        val routineRepository = AndroidRoutineRepository(sharedPreferences)
                        val scheduler = RoutineAlarmScheduler(alarmRepository)
                        
                        Log.d(TAG, "Repositories initialized, getting routine instance")
                        
                        // Debug: Check what's in shared preferences
                        val allInstances = routineInstanceRepository.getAllRoutineInstances()
                        Log.d(TAG, "Found ${allInstances.size} routine instances in storage:")
                        allInstances.forEach { instance ->
                            Log.d(TAG, "  - Instance ID: ${instance.id}, enabled: ${instance.isEnabled}, days: ${instance.daysOfWeek}")
                        }
                        
                        // Get the routine instance to check if it's a repeating schedule
                        val routineInstance = routineInstanceRepository.getRoutineInstance(info.routineInstanceId)
                        
                        if (routineInstance == null) {
                            Log.w(TAG, "Routine instance not found: ${info.routineInstanceId}")
                            return@launch
                        }
                        
                        Log.d(TAG, "Found routine instance: enabled=${routineInstance.isEnabled}, daysOfWeek=${routineInstance.daysOfWeek}")
                        
                        // If the routine instance has days of week set, it's a repeating schedule
                        if (routineInstance.isEnabled && routineInstance.daysOfWeek.isNotEmpty()) {
                            Log.d(TAG, "This is a repeating schedule, checking if this is the final step")
                            
                            // Get the routine to check total steps and determine if this is the final step
                            val routine = routineRepository.getRoutine(routineInstance.routineId)
                            
                            if (routine == null) {
                                Log.w(TAG, "Routine not found: ${routineInstance.routineId}")
                                return@launch
                            }
                            
                            val totalSteps = routine.timeIntervals.size
                            val isLastStep = info.stepIndex == (totalSteps - 1)
                            
                            Log.d(TAG, "Step ${info.stepIndex + 1} of $totalSteps (isLastStep: $isLastStep)")
                            
                            if (isLastStep) {
                                Log.d(TAG, "🏁 Final step completed! Rescheduling routine for next week...")
                                
                                // Reschedule the entire routine instance
                                val result = scheduler.scheduleRoutineInstance(routineInstance, routine)
                                if (result.isSuccess) {
                                    Log.d(TAG, "✅ Successfully rescheduled repeat routine for next week")
                                } else {
                                    Log.e(TAG, "❌ Failed to reschedule repeat routine: ${result.exceptionOrNull()}")
                                }
                            } else {
                                Log.d(TAG, "⏭️ Not the final step, waiting for step ${totalSteps} before rescheduling")
                            }
                        } else {
                            Log.d(TAG, "This is not a repeating schedule (enabled=${routineInstance.isEnabled}, daysOfWeek=${routineInstance.daysOfWeek})")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 Error during automatic rescheduling", e)
                    }
                }
            } ?: run {
                Log.d(TAG, "No routine alarm info found, skipping rescheduling")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create alarm notification", e)
        }
    }
    
    
    private fun getAlarmSoundUri(): Uri? {
        var alarmSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmSoundUri == null) {
            alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alarmSoundUri == null) {
                alarmSoundUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
            }
        }
        return alarmSoundUri
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (notificationManager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    ALARM_CHANNEL_ID,
                    "Alarm Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Full-screen alarm notifications"
                    enableVibration(true)
                    setBypassDnd(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    // Set alarm sound for the channel
                    setSound(getAlarmSoundUri(), AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build())
                }
                
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Created alarm notification channel with sound")
            }
        }
    }
}