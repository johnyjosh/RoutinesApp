package com.excalibur.routines.domain.usecase

import com.excalibur.routines.domain.model.RoutineWithDetails
import com.excalibur.routines.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRoutinesUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    operator fun invoke(): Flow<List<RoutineWithDetails>> =
        routineRepository.getAllRoutinesWithDetails()
}
