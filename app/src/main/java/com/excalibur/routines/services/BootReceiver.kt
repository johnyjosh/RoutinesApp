package com.excalibur.routines.services

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.excalibur.routines.data.repositories.AndroidAlarmRepository
import com.excalibur.routines.data.repositories.AndroidRoutineInstanceRepository
import com.excalibur.routines.data.repositories.AndroidRoutineRepository
import com.excalibur.routines.domain.services.RoutineAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "📱 BOOT_COMPLETED received - restoring routine alarms")
            
            // Create scope for coroutine operations
            val scope = CoroutineScope(Dispatchers.IO)
            
            scope.launch {
                try {
                    restoreRoutineAlarms(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore routine alarms after boot", e)
                }
            }
        }
    }
    
    private suspend fun restoreRoutineAlarms(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("routines_prefs", Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Create repositories
        val routineRepository = AndroidRoutineRepository(sharedPreferences)
        val routineInstanceRepository = AndroidRoutineInstanceRepository(sharedPreferences)
        val alarmRepository = AndroidAlarmRepository(context, alarmManager, sharedPreferences)
        
        // Create alarm scheduler
        val routineAlarmScheduler = RoutineAlarmScheduler(alarmRepository)
        
        // Get all enabled routine instances
        val routineInstances = routineInstanceRepository.getAllRoutineInstances()
            .filter { it.isEnabled }
        
        Log.d(TAG, "Found ${routineInstances.size} enabled routine instances to restore")
        
        // Reschedule alarms for each enabled instance
        routineInstances.forEach { instance ->
            try {
                val routine = routineRepository.getRoutine(instance.routineId)
                if (routine != null) {
                    Log.d(TAG, "Restoring alarms for routine instance: ${instance.id} (${instance.name})")
                    routineAlarmScheduler.scheduleRoutineInstance(instance, routine)
                } else {
                    Log.w(TAG, "Routine not found for instance: ${instance.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore alarms for instance: ${instance.id}", e)
            }
        }
        
        Log.d(TAG, "✅ Routine alarm restoration completed")
    }
}