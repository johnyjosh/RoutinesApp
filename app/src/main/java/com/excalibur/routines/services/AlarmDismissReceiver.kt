package com.excalibur.routines.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmDismissReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmDismissReceiver"
        private const val ALARM_NOTIFICATION_ID = 1001
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm dismissed via notification")
        
        // Stop alarm sound and vibration using the singleton manager
        AlarmSoundManager.stopAlarm()
        
        // Cancel the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
        
        Log.d(TAG, "Alarm stopped and notification dismissed")
    }
}