package com.excalibur.routines.domain.repository

import com.excalibur.routines.domain.model.RoutineSchedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getAllSchedules(): Flow<List<RoutineSchedule>>
    suspend fun getAllSchedulesOnce(): List<RoutineSchedule>
    suspend fun getScheduleById(scheduleId: String): RoutineSchedule?
    suspend fun saveSchedule(schedule: RoutineSchedule)
    suspend fun setScheduleEnabled(scheduleId: String, isEnabled: Boolean)
    suspend fun deleteSchedule(scheduleId: String)
}
