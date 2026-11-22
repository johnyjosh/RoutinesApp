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
    @Query("SELECT * FROM routines WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllRoutinesWithDetails(): Flow<List<RoutineWithDetailsRelation>>

    @Transaction
    @Query("SELECT * FROM routines WHERE isArchived = 1 ORDER BY archivedAt DESC")
    fun getArchivedRoutinesWithDetails(): Flow<List<RoutineWithDetailsRelation>>

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

    @Query("UPDATE routines SET isArchived = 1, archivedAt = :archivedAt WHERE id = :routineId")
    suspend fun archiveRoutine(routineId: String, archivedAt: Long = System.currentTimeMillis())

    @Query("UPDATE routines SET isArchived = 0, archivedAt = NULL WHERE id = :routineId")
    suspend fun restoreRoutine(routineId: String)

    @Query("DELETE FROM routines WHERE isArchived = 1 AND archivedAt < :thresholdTime")
    suspend fun purgeExpiredArchivedRoutines(thresholdTime: Long)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)
}
