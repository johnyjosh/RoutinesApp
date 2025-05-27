package com.example.routines

import android.app.PendingIntent
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
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val ACTION_START_ALARM = "com.example.routines.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.routines.ACTION_STOP_ALARM"
        const val ALARM_NOTIFICATION_ID = 123 // Must be unique
        const val ALARM_CHANNEL_ID = "alarm_channel_id" // Same as in MainActivity/AlarmReceiver
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We are not using binding
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmService", "AlarmService started with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmTime = intent.getStringExtra("ALARM_TIME") ?: "Alarm"
                startForegroundService(alarmTime)
                startAlarmSound()
                startVibration()
            }

            ACTION_STOP_ALARM -> {
                stopAlarm()
                stopSelf() // Stop the service itself
            }
        }
        return START_STICKY // If service is killed, try to restart it
    }

    private fun startForegroundService(alarmTime: String) {
        val stopSelf = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val pStopSelf = PendingIntent.getService(
            this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // This is the notification that will be shown for the foreground service
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setContentTitle("Alarm Ringing!")
            .setContentText("Time: $alarmTime - Tap to stop or snooze.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your alarm icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", pStopSelf) // Example stop action (TODO: replace with custom icon)
            // .setContentIntent(pOpenActivity) // Optional: Tap notification to open app/alarm screen
            .setOngoing(true) // Makes the notification non-dismissible by swiping
            .build()

        startForeground(ALARM_NOTIFICATION_ID, notification)
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