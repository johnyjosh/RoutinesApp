package com.excalibur.routines.data.repository

import com.excalibur.routines.data.local.dao.RoutineDao
import com.excalibur.routines.data.local.entity.RoutineEntity
import com.excalibur.routines.data.local.entity.RoutineStepEntity
import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.model.RoutineWithDetails
import com.excalibur.routines.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao
) : RoutineRepository {

    override fun getAllRoutinesWithDetails(): Flow<List<RoutineWithDetails>> {
        return routineDao.getAllRoutinesWithDetails().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getArchivedRoutinesWithDetails(): Flow<List<RoutineWithDetails>> {
        return routineDao.getArchivedRoutinesWithDetails().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRoutineWithDetails(routineId: String): Flow<RoutineWithDetails?> {
        return routineDao.getRoutineWithDetails(routineId).map { it?.toDomain() }
    }

    override suspend fun getRoutineWithDetailsOnce(routineId: String): RoutineWithDetails? {
        return routineDao.getRoutineWithDetailsOnce(routineId)?.toDomain()
    }

    override suspend fun saveRoutine(routine: Routine, steps: List<RoutineStep>) {
        val routineEntity = RoutineEntity.fromDomain(routine)
        val stepEntities = steps.mapIndexed { index, step ->
            RoutineStepEntity.fromDomain(step.copy(orderIndex = index, routineId = routine.id))
        }
        routineDao.saveRoutineWithSteps(routineEntity, stepEntities)
    }

    override suspend fun archiveRoutine(routineId: String) {
        routineDao.archiveRoutine(routineId, System.currentTimeMillis())
    }

    override suspend fun restoreRoutine(routineId: String) {
        routineDao.restoreRoutine(routineId)
    }

    override suspend fun permanentlyDeleteRoutine(routineId: String) {
        routineDao.deleteRoutine(routineId)
    }

    override suspend fun purgeExpiredRoutines(daysOld: Int) {
        val thresholdMillis = System.currentTimeMillis() - (daysOld.toLong() * 24L * 60L * 60L * 1000L)
        routineDao.purgeExpiredArchivedRoutines(thresholdMillis)
    }
}
