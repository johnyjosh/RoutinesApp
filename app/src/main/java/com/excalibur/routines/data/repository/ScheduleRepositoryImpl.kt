package com.excalibur.routines.data.repository

import com.excalibur.routines.data.local.dao.ScheduleDao
import com.excalibur.routines.data.local.entity.RoutineScheduleEntity
import com.excalibur.routines.domain.model.RoutineSchedule
import com.excalibur.routines.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao
) : ScheduleRepository {

    override fun getAllSchedules(): Flow<List<RoutineSchedule>> {
        return scheduleDao.getAllSchedules().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllSchedulesOnce(): List<RoutineSchedule> {
        return scheduleDao.getAllSchedulesOnce().map { it.toDomain() }
    }

    override suspend fun getScheduleById(scheduleId: String): RoutineSchedule? {
        return scheduleDao.getScheduleById(scheduleId)?.toDomain()
    }

    override suspend fun saveSchedule(schedule: RoutineSchedule) {
        scheduleDao.upsertSchedule(RoutineScheduleEntity.fromDomain(schedule))
    }

    override suspend fun setScheduleEnabled(scheduleId: String, isEnabled: Boolean) {
        scheduleDao.setScheduleEnabled(scheduleId, isEnabled)
    }

    override suspend fun deleteSchedule(scheduleId: String) {
        scheduleDao.deleteSchedule(scheduleId)
    }
}
