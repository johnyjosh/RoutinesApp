package com.excalibur.routines.domain.services

import com.excalibur.routines.domain.models.AlarmItem
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.RoutineInstance
import com.excalibur.routines.domain.repositories.AlarmRepository
import java.time.DayOfWeek

class RoutineAlarmScheduler(
    private val alarmRepository: AlarmRepository?
) {
    
    companion object {
        private const val ROUTINE_ALARM_PREFIX = "routine_"
        private const val ROUTINE_STEP_SEPARATOR = "_step_"
    }

    /**
     * Schedules all alarms for a routine instance
     * Creates alarms for the start time and each subsequent step
     */
    suspend fun scheduleRoutineInstance(
        routineInstance: RoutineInstance,
        routine: Routine
    ): Result<Unit> {
        if (alarmRepository == null) {
            return Result.failure(IllegalStateException("AlarmRepository not available"))
        }
        
        try {
            // Cancel any existing alarms for this routine instance
            cancelRoutineInstance(routineInstance)
            
            if (!routineInstance.isEnabled) {
                return Result.success(Unit)
            }
            
            if (routineInstance.daysOfWeek.isEmpty()) {
                // Schedule as one-time alarm
                scheduleOneTimeRoutine(routineInstance, routine).getOrThrow()
            } else {
                // Schedule alarms for each day of the week
                routineInstance.daysOfWeek.forEach { dayOfWeek ->
                    scheduleRoutineForDay(routineInstance, routine, dayOfWeek).getOrThrow()
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Cancels all alarms for a routine instance
     */
    suspend fun cancelRoutineInstance(routineInstance: RoutineInstance): Result<Unit> {
        if (alarmRepository == null) {
            return Result.failure(IllegalStateException("AlarmRepository not available"))
        }
        
        try {
            // Get all alarms and filter for this routine instance
            val allAlarms = alarmRepository.getAllAlarms()
            val routineAlarms = allAlarms.filter { alarm ->
                alarm.id.startsWith("${ROUTINE_ALARM_PREFIX}${routineInstance.id}")
            }
            
            // Cancel each alarm
            routineAlarms.forEach { alarm ->
                alarmRepository.cancelAlarm(alarm.id)
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Schedules a routine as a one-time alarm (no specific days)
     */
    private suspend fun scheduleOneTimeRoutine(
        routineInstance: RoutineInstance,
        routine: Routine
    ): Result<Unit> {
        var currentTime = routineInstance.startTime
        
        routine.timeIntervals.forEachIndexed { stepIndex, timeInterval ->
            val alarmId = generateOneTimeAlarmId(routineInstance.id, stepIndex)
            
            val alarmTitle = routineInstance.name
            
            val alarmDescription = if (stepIndex == 0) {
                routine.timeIntervals.firstOrNull()?.name ?: "Start routine"
            } else {
                timeInterval.name
            }
            
            val alarmItem = AlarmItem(
                id = alarmId,
                time = currentTime,
                title = alarmTitle,
                description = alarmDescription,
                isEnabled = true,
                isRepeating = false // One-time alarm
            )
            
            // Schedule the one-time alarm
            alarmRepository?.scheduleAlarm(alarmItem)?.getOrThrow()
            
            // Add the duration of this interval to get the next alarm time
            if (stepIndex < routine.timeIntervals.size - 1) {
                val durationMinutes = timeInterval.getDurationInMinutes()
                currentTime = currentTime.plusMinutes(durationMinutes)
            }
        }
        
        return Result.success(Unit)
    }

    /**
     * Schedules a routine for a specific day of the week
     */
    private suspend fun scheduleRoutineForDay(
        routineInstance: RoutineInstance,
        routine: Routine,
        dayOfWeek: DayOfWeek
    ): Result<Unit> {
        var currentTime = routineInstance.startTime
        
        routine.timeIntervals.forEachIndexed { stepIndex, timeInterval ->
            val alarmId = generateAlarmId(routineInstance.id, dayOfWeek, stepIndex)
            
            val alarmTitle = if (stepIndex == 0) {
                "Start: ${routine.name}"
            } else {
                "${routine.name} - ${timeInterval.name}"
            }
            
            val alarmDescription = if (stepIndex == 0) {
                routine.timeIntervals.firstOrNull()?.name ?: "Start routine"
            } else {
                timeInterval.name
            }
            
            val alarmItem = AlarmItem(
                id = alarmId,
                time = currentTime,
                title = alarmTitle,
                description = alarmDescription,
                isEnabled = true,
                isRepeating = true
            )
            
            // Schedule the alarm for this specific day
            scheduleAlarmForDay(alarmItem, dayOfWeek).getOrThrow()
            
            // Add the duration of this interval to get the next alarm time
            if (stepIndex < routine.timeIntervals.size - 1) {
                val durationMinutes = timeInterval.getDurationInMinutes()
                currentTime = currentTime.plusMinutes(durationMinutes)
            }
        }
        
        return Result.success(Unit)
    }

    /**
     * Schedules a single alarm for a specific day of the week
     */
    private suspend fun scheduleAlarmForDay(
        alarmItem: AlarmItem,
        dayOfWeek: DayOfWeek
    ): Result<Unit> {
        if (alarmRepository == null) {
            return Result.failure(IllegalStateException("AlarmRepository not available"))
        }
        
        // The alarm ID already contains the day-specific suffix from generateAlarmId
        val daySpecificAlarm = alarmItem
        
        return alarmRepository.scheduleAlarmForDay(daySpecificAlarm, dayOfWeek)
    }

    /**
     * Generates a unique alarm ID for a routine step
     */
    private fun generateAlarmId(
        routineInstanceId: String,
        dayOfWeek: DayOfWeek,
        stepIndex: Int
    ): String {
        return "${ROUTINE_ALARM_PREFIX}${routineInstanceId}${ROUTINE_STEP_SEPARATOR}${stepIndex}_${dayOfWeek.name.lowercase()}"
    }

    /**
     * Generates a unique alarm ID for a one-time routine step
     */
    private fun generateOneTimeAlarmId(
        routineInstanceId: String,
        stepIndex: Int
    ): String {
        return "${ROUTINE_ALARM_PREFIX}${routineInstanceId}${ROUTINE_STEP_SEPARATOR}${stepIndex}_onetime"
    }

    /**
     * Checks if an alarm ID belongs to a routine
     */
    fun isRoutineAlarm(alarmId: String): Boolean {
        return alarmId.startsWith(ROUTINE_ALARM_PREFIX)
    }

    /**
     * Extracts routine instance information from alarm ID
     */
    fun parseRoutineAlarmId(alarmId: String): RoutineAlarmInfo? {
        if (!isRoutineAlarm(alarmId)) return null
        
        try {
            val parts = alarmId.removePrefix(ROUTINE_ALARM_PREFIX).split(ROUTINE_STEP_SEPARATOR)
            if (parts.size != 2) return null
            
            val routineInstanceId = parts[0]
            val stepAndDay = parts[1].split("_")
            if (stepAndDay.size != 2) return null
            
            val stepIndex = stepAndDay[0].toInt()
            val dayOfWeek = DayOfWeek.valueOf(stepAndDay[1].uppercase())
            
            return RoutineAlarmInfo(routineInstanceId, stepIndex, dayOfWeek)
        } catch (e: Exception) {
            return null
        }
    }

}

/**
 * Information extracted from a routine alarm ID
 */
data class RoutineAlarmInfo(
    val routineInstanceId: String,
    val stepIndex: Int,
    val dayOfWeek: DayOfWeek
)