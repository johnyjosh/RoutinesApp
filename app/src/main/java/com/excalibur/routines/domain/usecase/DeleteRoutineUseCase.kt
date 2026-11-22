package com.excalibur.routines.domain.usecase

import com.excalibur.routines.domain.repository.AlarmScheduler
import com.excalibur.routines.domain.repository.RoutineRepository
import javax.inject.Inject

class DeleteRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(routineId: String) {
        val routine = routineRepository.getRoutineWithDetailsOnce(routineId)
        if (routine != null) {
            for (schedule in routine.schedules) {
                alarmScheduler.cancelRoutineSchedule(schedule.id)
            }
        }
        routineRepository.archiveRoutine(routineId)
    }
}
