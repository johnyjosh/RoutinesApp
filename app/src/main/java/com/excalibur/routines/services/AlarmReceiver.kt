package com.excalibur.routines.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.excalibur.routines.services.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 ALARM RECEIVED! Intent action: ${intent.action}")

        val alarmId = intent.getStringExtra("ALARM_ID") ?: "unknown"
        val alarmTime = intent.getStringExtra("ALARM_TIME") ?: "Alarm"
        val alarmTitle = intent.getStringExtra("ALARM_TITLE") ?: "Routine Time"

        Log.d(TAG, "Alarm details - ID: $alarmId, Time: $alarmTime, Title: $alarmTitle")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TIME", alarmTime)
            putExtra("ALARM_TITLE", alarmTitle)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service for alarm")
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                Log.d(TAG, "Starting regular service for alarm")
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm service", e)
        }
    }
}