package com.excalibur.routines.domain.repository

import com.excalibur.routines.domain.model.RoutineSchedule
import com.excalibur.routines.domain.model.RoutineWithDetails

interface AlarmScheduler {
    suspend fun scheduleRoutine(routineWithDetails: RoutineWithDetails, schedule: RoutineSchedule)
    suspend fun cancelRoutineSchedule(scheduleId: String)
    suspend fun rescheduleAll()
}
