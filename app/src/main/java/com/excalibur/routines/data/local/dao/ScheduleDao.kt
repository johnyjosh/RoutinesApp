package com.excalibur.routines.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.excalibur.routines.data.local.entity.RoutineScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM routine_schedules ORDER BY createdAt DESC")
    fun getAllSchedules(): Flow<List<RoutineScheduleEntity>>

    @Query("SELECT * FROM routine_schedules ORDER BY createdAt DESC")
    suspend fun getAllSchedulesOnce(): List<RoutineScheduleEntity>

    @Query("SELECT * FROM routine_schedules WHERE id = :id")
    suspend fun getScheduleById(id: String): RoutineScheduleEntity?

    @Query("SELECT * FROM routine_schedules WHERE routineId = :routineId")
    suspend fun getSchedulesForRoutine(routineId: String): List<RoutineScheduleEntity>

    @Upsert
    suspend fun upsertSchedule(schedule: RoutineScheduleEntity)

    @Query("UPDATE routine_schedules SET isEnabled = :isEnabled WHERE id = :scheduleId")
    suspend fun setScheduleEnabled(scheduleId: String, isEnabled: Boolean)

    @Query("DELETE FROM routine_schedules WHERE id = :scheduleId")
    suspend fun deleteSchedule(scheduleId: String)
}
