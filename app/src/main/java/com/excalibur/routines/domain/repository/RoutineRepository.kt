package com.excalibur.routines.domain.repository

import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.model.RoutineWithDetails
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getAllRoutinesWithDetails(): Flow<List<RoutineWithDetails>>
    fun getArchivedRoutinesWithDetails(): Flow<List<RoutineWithDetails>>
    fun getRoutineWithDetails(routineId: String): Flow<RoutineWithDetails?>
    suspend fun getRoutineWithDetailsOnce(routineId: String): RoutineWithDetails?
    suspend fun saveRoutine(routine: Routine, steps: List<RoutineStep>)
    suspend fun archiveRoutine(routineId: String)
    suspend fun restoreRoutine(routineId: String)
    suspend fun permanentlyDeleteRoutine(routineId: String)
    suspend fun purgeExpiredRoutines(daysOld: Int = 30)
}
