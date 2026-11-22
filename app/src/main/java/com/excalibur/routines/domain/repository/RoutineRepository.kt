package com.excalibur.routines.domain.repository

import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.model.RoutineWithDetails
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getAllRoutinesWithDetails(): Flow<List<RoutineWithDetails>>
    fun getRoutineWithDetails(routineId: String): Flow<RoutineWithDetails?>
    suspend fun getRoutineWithDetailsOnce(routineId: String): RoutineWithDetails?
    suspend fun saveRoutine(routine: Routine, steps: List<RoutineStep>)
    suspend fun deleteRoutine(routineId: String)
}
