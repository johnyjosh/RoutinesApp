package com.excalibur.routines.data.repositories

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.excalibur.routines.domain.models.AlarmItem
import com.excalibur.routines.domain.repositories.AlarmRepository
import com.excalibur.routines.services.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class AndroidAlarmRepository(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val sharedPreferences: SharedPreferences
) : AlarmRepository {

    companion object {
        private const val TAG = "AndroidAlarmRepository"
        private const val ALARMS_KEY = "scheduled_alarms"
    }

    override suspend fun scheduleTestAlarm(alarmItem: AlarmItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧪 Scheduling TEST alarm: ${alarmItem.id} at ${alarmItem.getTimeString()}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                return@withContext Result.failure(SecurityException("Cannot schedule exact alarms - please grant permission in device settings"))
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarmItem.id)
                putExtra("ALARM_TIME", alarmItem.getTimeString())
                putExtra("ALARM_TITLE", "${alarmItem.title} (TEST)")
            }

            val requestCode = alarmItem.id.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // For test alarms, use current time + alarm time offset (no next-day logic)
            val calendar = Calendar.getInstance().apply {
                add(Calendar.SECOND, 2) // Always 2 seconds from now
                set(Calendar.MILLISECOND, 0)
            }

            Log.d(TAG, "Scheduling TEST alarm for: ${calendar.time} (${calendar.timeInMillis})")
            Log.d(TAG, "Current time: ${Calendar.getInstance().time}")
            
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            // Don't save test alarms to preferences
            Log.d(TAG, "TEST alarm successfully scheduled for ${alarmItem.getTimeString()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test alarm", e)
            Result.failure(e)
        }
    }

    override suspend fun scheduleAlarm(alarmItem: AlarmItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to schedule alarm: ${alarmItem.id} at ${alarmItem.getTimeString()}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                return@withContext Result.failure(SecurityException("Cannot schedule exact alarms - please grant permission in device settings"))
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarmItem.id)
                putExtra("ALARM_TIME", alarmItem.getTimeString())
                putExtra("ALARM_TITLE", alarmItem.title)
            }

            val requestCode = alarmItem.id.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarmItem.time.hour)
                set(Calendar.MINUTE, alarmItem.time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                    Log.d(TAG, "Alarm time is in the past, scheduling for tomorrow")
                }
            }

            Log.d(TAG, "Scheduling alarm for: ${calendar.time} (${calendar.timeInMillis})")
            Log.d(TAG, "Current time: ${Calendar.getInstance().time}")
            
            if (alarmItem.isRepeating) {
                // For repeating daily alarms, use setRepeating with daily interval
                Log.d(TAG, "Setting up daily recurring alarm")
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY, // Daily interval
                    pendingIntent
                )
            } else {
                // For one-time alarms, use setExactAndAllowWhileIdle
                Log.d(TAG, "Setting up one-time alarm")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            saveAlarmToPreferences(alarmItem)
            Log.d(TAG, "Alarm successfully scheduled for ${alarmItem.getTimeString()} (repeating: ${alarmItem.isRepeating})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
            Result.failure(e)
        }
    }

    override suspend fun scheduleAlarmForDay(alarmItem: AlarmItem, dayOfWeek: DayOfWeek): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to schedule alarm: ${alarmItem.id} at ${alarmItem.getTimeString()} for ${dayOfWeek.name}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms - permission not granted")
                return@withContext Result.failure(SecurityException("Cannot schedule exact alarms - please grant permission in device settings"))
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarmItem.id)
                putExtra("ALARM_TIME", alarmItem.getTimeString())
                putExtra("ALARM_TITLE", alarmItem.title)
            }

            val requestCode = alarmItem.id.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Calculate the next occurrence of the specified day of week
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarmItem.time.hour)
                set(Calendar.MINUTE, alarmItem.time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // Convert DayOfWeek to Calendar day (Java time uses Monday=1, Calendar uses Sunday=1)
                val targetCalendarDay = when (dayOfWeek) {
                    DayOfWeek.SUNDAY -> Calendar.SUNDAY
                    DayOfWeek.MONDAY -> Calendar.MONDAY
                    DayOfWeek.TUESDAY -> Calendar.TUESDAY
                    DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                    DayOfWeek.THURSDAY -> Calendar.THURSDAY
                    DayOfWeek.FRIDAY -> Calendar.FRIDAY
                    DayOfWeek.SATURDAY -> Calendar.SATURDAY
                }
                
                val currentDay = get(Calendar.DAY_OF_WEEK)
                val currentTime = Calendar.getInstance()
                
                // If it's the same day and time hasn't passed, schedule for today
                if (currentDay == targetCalendarDay && after(currentTime)) {
                    // Keep current date
                    Log.d(TAG, "Scheduling for today (${dayOfWeek.name})")
                } else {
                    // Find the next occurrence of this day
                    var daysToAdd = targetCalendarDay - currentDay
                    if (daysToAdd <= 0) {
                        daysToAdd += 7 // Schedule for next week
                    }
                    add(Calendar.DATE, daysToAdd)
                    Log.d(TAG, "Scheduling for next ${dayOfWeek.name} in $daysToAdd days")
                }
            }

            Log.d(TAG, "Scheduling alarm for: ${calendar.time} (${calendar.timeInMillis})")
            Log.d(TAG, "Current time: ${Calendar.getInstance().time}")
            
            if (alarmItem.isRepeating) {
                // For repeating alarms, use setRepeating with weekly interval
                Log.d(TAG, "Setting up weekly recurring alarm for ${dayOfWeek.name}")
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7, // Weekly interval
                    pendingIntent
                )
            } else {
                // For one-time alarms, use setExactAndAllowWhileIdle
                Log.d(TAG, "Setting up one-time alarm")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            saveAlarmToPreferences(alarmItem)
            Log.d(TAG, "Alarm successfully scheduled for ${alarmItem.getTimeString()} on ${dayOfWeek.name} (repeating: ${alarmItem.isRepeating})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for day", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelAlarm(alarmId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = alarmId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            removeAlarmFromPreferences(alarmId)
            Log.d(TAG, "Alarm cancelled: $alarmId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllAlarms(): List<AlarmItem> = withContext(Dispatchers.IO) {
        return@withContext loadAlarmsFromPreferences()
    }

    override suspend fun getAlarm(alarmId: String): AlarmItem? = withContext(Dispatchers.IO) {
        return@withContext loadAlarmsFromPreferences().find { it.id == alarmId }
    }

    override suspend fun updateAlarm(alarmItem: AlarmItem): Result<Unit> = withContext(Dispatchers.IO) {
        cancelAlarm(alarmItem.id)
        if (alarmItem.isEnabled) {
            scheduleAlarm(alarmItem)
        } else {
            saveAlarmToPreferences(alarmItem)
            Result.success(Unit)
        }
    }

    override suspend fun deleteAlarm(alarmId: String): Result<Unit> = withContext(Dispatchers.IO) {
        cancelAlarm(alarmId)
    }

    private fun saveAlarmToPreferences(alarmItem: AlarmItem) {
        val alarms = loadAlarmsFromPreferences().toMutableList()
        alarms.removeAll { it.id == alarmItem.id }
        alarms.add(alarmItem)
        
        val alarmStrings = alarms.map { alarm ->
            "${alarm.id}|${alarm.time.format(DateTimeFormatter.ofPattern("HH:mm"))}|${alarm.title}|${alarm.isEnabled}|${alarm.isRepeating}"
        }.toSet()
        
        sharedPreferences.edit()
            .putStringSet(ALARMS_KEY, alarmStrings)
            .apply()
    }

    private fun removeAlarmFromPreferences(alarmId: String) {
        val alarms = loadAlarmsFromPreferences().toMutableList()
        alarms.removeAll { it.id == alarmId }
        
        val alarmStrings = alarms.map { alarm ->
            "${alarm.id}|${alarm.time.format(DateTimeFormatter.ofPattern("HH:mm"))}|${alarm.title}|${alarm.isEnabled}|${alarm.isRepeating}"
        }.toSet()
        
        sharedPreferences.edit()
            .putStringSet(ALARMS_KEY, alarmStrings)
            .apply()
    }

    private fun loadAlarmsFromPreferences(): List<AlarmItem> {
        val alarmStrings = sharedPreferences.getStringSet(ALARMS_KEY, emptySet()) ?: emptySet()
        return alarmStrings.mapNotNull { alarmString ->
            try {
                val parts = alarmString.split("|")
                if (parts.size >= 5) {
                    AlarmItem(
                        id = parts[0],
                        time = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm")),
                        title = parts[2],
                        isEnabled = parts[3].toBoolean(),
                        isRepeating = parts[4].toBoolean()
                    )
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse alarm from preferences: $alarmString", e)
                null
            }
        }
    }
}