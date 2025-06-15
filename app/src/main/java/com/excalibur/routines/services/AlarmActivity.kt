package com.excalibur.routines.services

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AlarmActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "AlarmActivity"
        private const val ALARM_NOTIFICATION_ID = 1001
    }
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚨 AlarmActivity started")
        
        // Make this activity show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Dismiss keyguard
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
        
        // Get alarm details from intent
        val alarmId = intent.getStringExtra("ALARM_ID") ?: "unknown"
        val alarmTime = intent.getStringExtra("ALARM_TIME") ?: "Alarm"
        val alarmTitle = intent.getStringExtra("ALARM_TITLE") ?: "Routine Time"
        val alarmDescription = intent.getStringExtra("ALARM_DESCRIPTION") ?: ""
        
        Log.d(TAG, "Alarm details - ID: $alarmId, Time: $alarmTime, Title: $alarmTitle")
        
        // Cancel the notification since we're launching the full activity
        dismissNotification()
        
        // The AlarmSoundManager is already playing from the receiver
        // Just ensure it continues playing (no need to restart)
        
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme()
            ) {
                AlarmScreen(
                    title = alarmTitle,
                    description = alarmDescription,
                    time = alarmTime,
                    onDismiss = {
                        Log.d(TAG, "Alarm dismissed by user")
                        AlarmSoundManager.stopAlarm()
                        dismissNotification()
                        finish()
                    }
                )
            }
        }
    }
    
    
    private fun dismissNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
        Log.d(TAG, "Dismissed alarm notification")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        AlarmSoundManager.stopAlarm()
        dismissNotification()
    }
}

@Composable
fun AlarmScreen(
    title: String,
    description: String,
    time: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔔",
                fontSize = 72.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DISMISS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}