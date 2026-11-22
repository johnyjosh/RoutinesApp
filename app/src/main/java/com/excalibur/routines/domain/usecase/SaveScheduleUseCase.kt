package com.excalibur.routines.domain.usecase

import com.excalibur.routines.domain.model.RoutineSchedule
import com.excalibur.routines.domain.repository.AlarmScheduler
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.ScheduleRepository
import javax.inject.Inject

class SaveScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(schedule: RoutineSchedule) {
        scheduleRepository.saveSchedule(schedule)
        val routine = routineRepository.getRoutineWithDetailsOnce(schedule.routineId)
        if (routine != null && schedule.isEnabled) {
            alarmScheduler.scheduleRoutine(routine, schedule)
        } else {
            alarmScheduler.cancelRoutineSchedule(schedule.id)
        }
    }
}
