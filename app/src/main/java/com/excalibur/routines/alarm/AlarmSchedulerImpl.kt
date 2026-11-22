package com.excalibur.routines.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.excalibur.routines.MainActivity
import com.excalibur.routines.domain.model.RoutineSchedule
import com.excalibur.routines.domain.model.RoutineWithDetails
import com.excalibur.routines.domain.repository.AlarmScheduler
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.ScheduleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineRepository: RoutineRepository,
    private val scheduleRepository: ScheduleRepository
) : AlarmScheduler {

    companion object {
        private const val TAG = "AlarmSchedulerImpl"
        const val ACTION_ROUTINE_ALARM = "com.excalibur.routines.ACTION_ROUTINE_ALARM"

        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_ROUTINE_NAME = "extra_routine_name"
        const val EXTRA_ROUTINE_ICON = "extra_routine_icon"
        const val EXTRA_STEP_ID = "extra_step_id"
        const val EXTRA_STEP_NAME = "extra_step_name"
        const val EXTRA_STEP_INDEX = "extra_step_index"
        const val EXTRA_TOTAL_STEPS = "extra_total_steps"
        const val EXTRA_NEXT_STEP_NAME = "extra_next_step_name"
        const val EXTRA_SOUND_ENABLED = "extra_sound_enabled"
        const val EXTRA_VIBRATE_ENABLED = "extra_vibrate_enabled"
        const val EXTRA_IS_FORCEFUL = "extra_is_forceful"
        const val EXTRA_IS_COMPLETION = "extra_is_completion"
        const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"

        fun generateRequestCode(scheduleId: String, dayOfWeek: DayOfWeek?, stepIndex: Int): Int {
            val dayVal = dayOfWeek?.value ?: 0
            return ((scheduleId.hashCode() * 31 + dayVal * 17 + stepIndex) and 0x7FFFFFFF)
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleRoutine(
        routineWithDetails: RoutineWithDetails,
        schedule: RoutineSchedule
    ) {
        cancelRoutineSchedule(schedule.id)

        if (!schedule.isEnabled) {
            Log.d(TAG, "Schedule ${schedule.id} is disabled, skipping alarm registration")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarms: permission missing")
            return
        }

        val steps = routineWithDetails.steps
        val days = if (schedule.daysOfWeek.isEmpty()) listOf(null) else schedule.daysOfWeek.toList()

        days.forEach { dayOfWeek ->
            var offsetSeconds = 0
            val totalSteps = if (steps.isEmpty()) 1 else steps.size

            if (steps.isEmpty()) {
                val triggerTime = calculateNextTriggerTime(dayOfWeek, schedule.startHour, schedule.startMinute, 0)
                scheduleStepAlarm(
                    schedule = schedule,
                    routineWithDetails = routineWithDetails,
                    dayOfWeek = dayOfWeek,
                    stepIndex = 0,
                    totalSteps = 1,
                    stepId = "default",
                    stepName = routineWithDetails.routine.name,
                    nextStepName = null,
                    isForceful = true,
                    isCompletion = false,
                    triggerTimeMs = triggerTime
                )
            } else {
                steps.forEachIndexed { index, step ->
                    val triggerTime = calculateNextTriggerTime(
                        dayOfWeek = dayOfWeek,
                        hour = schedule.startHour,
                        minute = schedule.startMinute,
                        offsetSeconds = offsetSeconds
                    )

                    val nextStepName = steps.getOrNull(index + 1)?.name

                    scheduleStepAlarm(
                        schedule = schedule,
                        routineWithDetails = routineWithDetails,
                        dayOfWeek = dayOfWeek,
                        stepIndex = index,
                        totalSteps = totalSteps,
                        stepId = step.id,
                        stepName = step.name,
                        nextStepName = nextStepName,
                        isForceful = step.isForcefulAlarm,
                        isCompletion = false,
                        triggerTimeMs = triggerTime
                    )

                    offsetSeconds += step.totalSeconds
                }

                // Schedule Final Completion Alarm when the entire routine finishes
                val completionTriggerTime = calculateNextTriggerTime(
                    dayOfWeek = dayOfWeek,
                    hour = schedule.startHour,
                    minute = schedule.startMinute,
                    offsetSeconds = offsetSeconds
                )

                scheduleStepAlarm(
                    schedule = schedule,
                    routineWithDetails = routineWithDetails,
                    dayOfWeek = dayOfWeek,
                    stepIndex = steps.size,
                    totalSteps = totalSteps,
                    stepId = "completion",
                    stepName = "Routine Finished! 🎉",
                    nextStepName = null,
                    isForceful = false, // Gentle completion chime by default
                    isCompletion = true,
                    triggerTimeMs = completionTriggerTime
                )
            }
        }
    }

    override suspend fun cancelRoutineSchedule(scheduleId: String) {
        val days = DayOfWeek.entries.toList() + listOf(null)
        for (day in days) {
            for (stepIndex in 0..50) {
                val requestCode = generateRequestCode(scheduleId, day, stepIndex)
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ROUTINE_ALARM
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }
    }

    override suspend fun rescheduleAll() {
        Log.d(TAG, "Rescheduling all enabled routines...")
        val schedules = scheduleRepository.getAllSchedulesOnce()
        for (schedule in schedules) {
            if (schedule.isEnabled) {
                val routineWithDetails = routineRepository.getRoutineWithDetailsOnce(schedule.routineId)
                if (routineWithDetails != null) {
                    scheduleRoutine(routineWithDetails, schedule)
                }
            }
        }
    }

    private fun scheduleStepAlarm(
        schedule: RoutineSchedule,
        routineWithDetails: RoutineWithDetails,
        dayOfWeek: DayOfWeek?,
        stepIndex: Int,
        totalSteps: Int,
        stepId: String,
        stepName: String,
        nextStepName: String?,
        isForceful: Boolean,
        isCompletion: Boolean,
        triggerTimeMs: Long
    ) {
        val requestCode = generateRequestCode(schedule.id, dayOfWeek, stepIndex)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ROUTINE_ALARM
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_ROUTINE_ID, routineWithDetails.routine.id)
            putExtra(EXTRA_ROUTINE_NAME, routineWithDetails.routine.name)
            putExtra(EXTRA_ROUTINE_ICON, routineWithDetails.routine.icon)
            putExtra(EXTRA_STEP_ID, stepId)
            putExtra(EXTRA_STEP_NAME, stepName)
            putExtra(EXTRA_STEP_INDEX, stepIndex)
            putExtra(EXTRA_TOTAL_STEPS, totalSteps)
            putExtra(EXTRA_NEXT_STEP_NAME, nextStepName)
            putExtra(EXTRA_SOUND_ENABLED, schedule.soundEnabled)
            putExtra(EXTRA_VIBRATE_ENABLED, schedule.vibrateEnabled)
            putExtra(EXTRA_IS_FORCEFUL, isForceful)
            putExtra(EXTRA_IS_COMPLETION, isCompletion)
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek?.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Log.d(TAG, "Scheduled alarm clock for '${routineWithDetails.routine.name} - $stepName' (Step $stepIndex) at epoch $triggerTimeMs")
    }

    private fun calculateNextTriggerTime(
        dayOfWeek: DayOfWeek?,
        hour: Int,
        minute: Int,
        offsetSeconds: Int
    ): Long {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            .plusSeconds(offsetSeconds.toLong())

        if (dayOfWeek != null) {
            var daysToAdd = (dayOfWeek.value - target.dayOfWeek.value) % 7
            if (daysToAdd < 0) daysToAdd += 7
            target = target.plusDays(daysToAdd.toLong())
            if (!target.isAfter(now)) {
                target = target.plusWeeks(1)
            }
        } else {
            if (!target.isAfter(now)) {
                target = target.plusDays(1)
            }
        }
        return target.toInstant().toEpochMilli()
    }
}
