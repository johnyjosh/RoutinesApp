package com.excalibur.routines.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val WAKE_LOCK_TAG = "RoutinesApp:AlarmReceiverWakeLock"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🚨 Alarm received with action: ${intent.action}")

        if (intent.action != AlarmSchedulerImpl.ACTION_ROUTINE_ALARM) {
            return
        }

        // Acquire quick wake lock to guarantee CPU doesn't sleep before Service starts
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(15 * 1000L) // 15 seconds
        }

        try {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_ALARM
                putExtras(intent)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground AlarmService", e)
        } finally {
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release wake lock", e)
            }
        }
    }
}
