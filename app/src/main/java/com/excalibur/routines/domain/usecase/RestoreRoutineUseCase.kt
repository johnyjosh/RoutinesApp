package com.excalibur.routines.domain.usecase

import com.excalibur.routines.domain.repository.AlarmScheduler
import com.excalibur.routines.domain.repository.RoutineRepository
import javax.inject.Inject

class RestoreRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(routineId: String) {
        routineRepository.restoreRoutine(routineId)
        val routine = routineRepository.getRoutineWithDetailsOnce(routineId)
        if (routine != null) {
            for (schedule in routine.schedules) {
                if (schedule.isEnabled) {
                    alarmScheduler.scheduleRoutine(routine, schedule)
                }
            }
        }
    }
}
