package com.excalibur.routines.domain.repositories

import com.excalibur.routines.domain.models.Routine

interface RoutineRepository {
    suspend fun createRoutine(routine: Routine): Result<Unit>
    suspend fun getAllRoutines(): List<Routine>
    suspend fun getRoutine(routineId: String): Routine?
    suspend fun updateRoutine(routine: Routine): Result<Unit>
    suspend fun deleteRoutine(routineId: String): Result<Unit>
    suspend fun duplicateRoutine(routineId: String, newName: String): Result<Routine>
}