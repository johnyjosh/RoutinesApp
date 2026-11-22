package com.excalibur.routines.domain.usecase

import com.excalibur.routines.domain.repository.AlarmScheduler
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.ScheduleRepository
import javax.inject.Inject

class ToggleScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(scheduleId: String, isEnabled: Boolean) {
        scheduleRepository.setScheduleEnabled(scheduleId, isEnabled)
        val schedule = scheduleRepository.getScheduleById(scheduleId) ?: return
        val routine = routineRepository.getRoutineWithDetailsOnce(schedule.routineId) ?: return

        if (isEnabled) {
            alarmScheduler.scheduleRoutine(routine, schedule)
        } else {
            alarmScheduler.cancelRoutineSchedule(scheduleId)
        }
    }
}
