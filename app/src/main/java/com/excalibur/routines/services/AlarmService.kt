package com.excalibur.routines.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import com.excalibur.routines.data.managers.AppNotificationManager

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var notificationManager: AppNotificationManager

    companion object {
        const val ACTION_START_ALARM = "com.excalibur.routines.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.excalibur.routines.ACTION_STOP_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = AppNotificationManager(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We are not using binding
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmService", "🚨 AlarmService started with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmId = intent.getStringExtra("ALARM_ID") ?: "unknown"
                val alarmTime = intent.getStringExtra("ALARM_TIME") ?: "Alarm"
                val alarmTitle = intent.getStringExtra("ALARM_TITLE") ?: "Routine Time"
                Log.d("AlarmService", "Starting alarm - ID: $alarmId, Time: $alarmTime, Title: $alarmTitle")
                
                try {
                    startForegroundService(alarmTime, alarmTitle)
                    startAlarmSound()
                    startVibration()
                    Log.d("AlarmService", "Alarm started successfully")
                } catch (e: Exception) {
                    Log.e("AlarmService", "Failed to start alarm", e)
                }
            }

            ACTION_STOP_ALARM -> {
                Log.d("AlarmService", "Stopping alarm")
                stopAlarm()
                stopSelf()
            }
            
            else -> {
                Log.w("AlarmService", "Unknown action: ${intent?.action}")
            }
        }
        return START_STICKY
    }

    private fun startForegroundService(alarmTime: String, alarmTitle: String) {
        val notification = notificationManager.createAlarmNotification(alarmTime, alarmTitle)
        startForeground(AppNotificationManager.ALARM_NOTIFICATION_ID, notification)
        Log.d("AlarmService", "Foreground service started.")
    }

    private fun startAlarmSound() {
        // Stop any previous playback
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        // Option 1: Default Alarm Sound
        var alarmSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmSoundUri == null) {
            // Fallback to notification sound if alarm is not set
            alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alarmSoundUri == null) {
                // Fallback to system default if nothing is set (rare)
                alarmSoundUri = Settings.System.DEFAULT_RINGTONE_URI
            }
        }

        // Option 2: Custom sound from res/raw (Uncomment to use)
        // val customSoundUri = Uri.parse("android.resource://${packageName}/${R.raw.your_alarm_sound_file_name}")
        // alarmSoundUri = customSoundUri

        if (alarmSoundUri != null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmSoundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true // Loop the sound
                prepareAsync() // Prepare asynchronously
                setOnPreparedListener {
                    Log.d("AlarmService", "MediaPlayer prepared, starting playback.")
                    start()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("AlarmService", "MediaPlayer error: what $what, extra $extra")
                    // Handle error, maybe stop service or try fallback
                    true // True if the error has been handled
                }
            }
        } else {
            Log.e("AlarmService", "Could not find an alarm sound URI.")
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 1000, 500) // Vibrate for 1s, pause for 0.5s
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 for repeat
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0) // 0 for repeat
            }
            Log.d("AlarmService", "Vibration started.")
        } else {
            Log.d("AlarmService", "Device does not have a vibrator or permission denied.")
        }
    }

    private fun stopAlarm() {
        Log.d("AlarmService", "Stopping alarm sound and vibration.")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm() // Ensure everything is cleaned up
        Log.d("AlarmService", "AlarmService destroyed.")
    }
}