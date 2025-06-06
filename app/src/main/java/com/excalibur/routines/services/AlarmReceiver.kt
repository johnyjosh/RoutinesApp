package com.excalibur.routines.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.excalibur.routines.services.AlarmService
import com.excalibur.routines.domain.services.RoutineAlarmScheduler

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

        // Check if this is a routine alarm
        val routineAlarmScheduler = RoutineAlarmScheduler(null) // We only need parsing functionality
        val routineAlarmInfo = routineAlarmScheduler.parseRoutineAlarmId(alarmId)
        
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TIME", alarmTime)
            putExtra("ALARM_TITLE", alarmTitle)
            
            // Add routine-specific information if this is a routine alarm
            routineAlarmInfo?.let { info ->
                putExtra("IS_ROUTINE_ALARM", true)
                putExtra("ROUTINE_INSTANCE_ID", info.routineInstanceId)
                putExtra("ROUTINE_STEP_INDEX", info.stepIndex)
                putExtra("ROUTINE_DAY_OF_WEEK", info.dayOfWeek.name)
                
                val stepDescription = if (info.stepIndex == 0) {
                    "Starting routine"
                } else {
                    "Step ${info.stepIndex}"
                }
                putExtra("ROUTINE_STEP_DESCRIPTION", stepDescription)
                
                Log.d(TAG, "🏃 Routine alarm - Instance: ${info.routineInstanceId}, Step: ${info.stepIndex}, Day: ${info.dayOfWeek}")
            }
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