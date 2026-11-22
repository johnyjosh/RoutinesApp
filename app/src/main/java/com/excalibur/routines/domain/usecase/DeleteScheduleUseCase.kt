package com.excalibur.routines.domain.usecase

import com.excalibur.routines.domain.repository.AlarmScheduler
import com.excalibur.routines.domain.repository.ScheduleRepository
import javax.inject.Inject

class DeleteScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(scheduleId: String) {
        alarmScheduler.cancelRoutineSchedule(scheduleId)
        scheduleRepository.deleteSchedule(scheduleId)
    }
}
