package com.excalibur.routines.domain.usecase

import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.repository.AlarmScheduler
import com.excalibur.routines.domain.repository.RoutineRepository
import javax.inject.Inject

class SaveRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(routine: Routine, steps: List<RoutineStep>) {
        routineRepository.saveRoutine(routine, steps)
        val updated = routineRepository.getRoutineWithDetailsOnce(routine.id)
        if (updated != null) {
            for (schedule in updated.schedules) {
                if (schedule.isEnabled) {
                    alarmScheduler.scheduleRoutine(updated, schedule)
                }
            }
        }
    }
}
