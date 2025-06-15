package com.excalibur.routines.services

import android.content.Context
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
import android.provider.Settings
import android.util.Log

/**
 * Singleton class to manage alarm sound and vibration across app components
 * Ensures alarm plays reliably regardless of UI state
 */
object AlarmSoundManager {
    
    private const val TAG = "AlarmSoundManager"
    private const val WAKE_LOCK_TAG = "RoutinesApp:AlarmWakeLock"
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPlaying = false
    
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "Audio focus changed: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Don't stop alarm on focus loss - alarm should override everything
                Log.d(TAG, "Audio focus lost, but keeping alarm playing")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Don't pause alarm for transient loss
                Log.d(TAG, "Transient audio focus loss, keeping alarm playing")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                ensureAlarmPlaying()
            }
        }
    }
    
    fun startAlarm(context: Context) {
        if (isPlaying) {
            Log.d(TAG, "Alarm already playing")
            return
        }
        
        Log.d(TAG, "Starting alarm sound and vibration")
        
        // Acquire wake lock to keep device awake
        acquireWakeLock(context)
        
        // Request audio focus for alarm
        requestAudioFocus(context)
        
        // Start alarm sound
        startAlarmSound(context)
        
        // Start vibration
        startVibration(context)
        
        isPlaying = true
    }
    
    fun stopAlarm() {
        Log.d(TAG, "Stopping alarm")
        
        isPlaying = false
        
        // Stop media player
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping media player", e)
            }
        }
        mediaPlayer = null
        
        // Stop vibration
        vibrator?.cancel()
        vibrator = null
        
        // Release audio focus
        releaseAudioFocus()
        
        // Release wake lock
        releaseWakeLock()
    }
    
    private fun acquireWakeLock(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                WAKE_LOCK_TAG
            )
            wakeLock?.acquire(60_000L) // 1 minute max
            Log.d(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            try {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing wake lock", e)
            }
        }
        wakeLock = null
    }
    
    private fun requestAudioFocus(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
                
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
                
            val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
            Log.d(TAG, "Audio focus request result: $result")
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN
            )
            Log.d(TAG, "Audio focus request result (legacy): $result")
        }
    }
    
    private fun releaseAudioFocus() {
        audioManager?.let { manager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    manager.abandonAudioFocusRequest(request)
                }
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(audioFocusChangeListener)
            }
            Log.d(TAG, "Audio focus released")
        }
        audioManager = null
        audioFocusRequest = null
    }
    
    private fun startAlarmSound(context: Context) {
        try {
            // Stop any existing playback
            mediaPlayer?.stop()
            mediaPlayer?.release()
            
            // Get alarm sound URI
            var alarmSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmSoundUri == null) {
                alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (alarmSoundUri == null) {
                    alarmSoundUri = Settings.System.DEFAULT_RINGTONE_URI
                }
            }
            
            if (alarmSoundUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alarmSoundUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    isLooping = true
                    setVolume(1.0f, 1.0f) // Max volume
                    prepareAsync()
                    setOnPreparedListener {
                        Log.d(TAG, "MediaPlayer prepared, starting alarm sound")
                        start()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what $what, extra $extra")
                        true
                    }
                }
            } else {
                Log.w(TAG, "No alarm sound URI available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound", e)
        }
    }
    
    private fun startVibration(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator?.hasVibrator() == true) {
                val pattern = longArrayOf(0, 1000, 500) // Vibrate for 1s, pause 0.5s, repeat
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
                Log.d(TAG, "Vibration started")
            } else {
                Log.d(TAG, "Device does not support vibration")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }
    
    private fun ensureAlarmPlaying() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                try {
                    player.start()
                    Log.d(TAG, "Restarted alarm playback")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restart alarm playback", e)
                }
            }
        }
    }
    
    fun isAlarmPlaying(): Boolean = isPlaying
}