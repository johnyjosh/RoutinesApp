package com.excalibur.routines.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.excalibur.routines.data.local.entity.RoutineEntity
import com.excalibur.routines.data.local.entity.RoutineStepEntity
import com.excalibur.routines.data.local.entity.RoutineWithDetailsRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Transaction
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun getAllRoutinesWithDetails(): Flow<List<RoutineWithDetailsRelation>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun getRoutineWithDetails(routineId: String): Flow<RoutineWithDetailsRelation?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineWithDetailsOnce(routineId: String): RoutineWithDetailsRelation?

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    suspend fun deleteStepsForRoutine(routineId: String)

    @Upsert
    suspend fun upsertSteps(steps: List<RoutineStepEntity>)

    @Transaction
    suspend fun saveRoutineWithSteps(routine: RoutineEntity, steps: List<RoutineStepEntity>) {
        upsertRoutine(routine)
        deleteStepsForRoutine(routine.id)
        if (steps.isNotEmpty()) {
            upsertSteps(steps)
        }
    }

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)
}
