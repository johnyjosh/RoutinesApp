package com.excalibur.routines.domain.repositories

import com.excalibur.routines.domain.models.RoutineInstance

interface RoutineInstanceRepository {
    suspend fun createRoutineInstance(routineInstance: RoutineInstance): Result<Unit>
    suspend fun getAllRoutineInstances(): List<RoutineInstance>
    suspend fun getRoutineInstance(instanceId: String): RoutineInstance?
    suspend fun getRoutineInstancesForRoutine(routineId: String): List<RoutineInstance>
    suspend fun updateRoutineInstance(routineInstance: RoutineInstance): Result<Unit>
    suspend fun deleteRoutineInstance(instanceId: String): Result<Unit>
    suspend fun deleteRoutineInstancesForRoutine(routineId: String): Result<Unit>
}