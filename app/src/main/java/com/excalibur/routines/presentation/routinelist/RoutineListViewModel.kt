package com.excalibur.routines.presentation.routinelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.model.RoutineWithDetails
import com.excalibur.routines.domain.model.UserStats
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.UserStatsRepository
import com.excalibur.routines.domain.usecase.DeleteRoutineUseCase
import com.excalibur.routines.domain.usecase.DeleteScheduleUseCase
import com.excalibur.routines.domain.usecase.GetRoutinesUseCase
import com.excalibur.routines.domain.usecase.RestoreRoutineUseCase
import com.excalibur.routines.domain.usecase.SaveRoutineUseCase
import com.excalibur.routines.domain.usecase.ToggleScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RoutineListUiState(
    val routines: List<RoutineWithDetails> = emptyList(),
    val archivedRoutines: List<RoutineWithDetails> = emptyList(),
    val userStats: UserStats = UserStats(),
    val isLoading: Boolean = false
)

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    getRoutinesUseCase: GetRoutinesUseCase,
    private val routineRepository: RoutineRepository,
    private val userStatsRepository: UserStatsRepository,
    private val saveRoutineUseCase: SaveRoutineUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
    private val restoreRoutineUseCase: RestoreRoutineUseCase,
    private val toggleScheduleUseCase: ToggleScheduleUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    init {
        // Auto-purge any routines that have been in the trash for over 30 days
        viewModelScope.launch {
            routineRepository.purgeExpiredRoutines(daysOld = 30)
        }
    }

    val uiState: StateFlow<RoutineListUiState> = combine(
        getRoutinesUseCase(),
        routineRepository.getArchivedRoutinesWithDetails(),
        userStatsRepository.getUserStats()
    ) { activeRoutines, archivedRoutines, stats ->
        RoutineListUiState(
            routines = activeRoutines,
            archivedRoutines = archivedRoutines,
            userStats = stats,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoutineListUiState(isLoading = true)
    )

    fun toggleSchedule(scheduleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            toggleScheduleUseCase(scheduleId, isEnabled)
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            deleteRoutineUseCase(routineId)
        }
    }

    fun restoreRoutine(routineId: String) {
        viewModelScope.launch {
            restoreRoutineUseCase(routineId)
        }
    }

    fun permanentlyDeleteRoutine(routineId: String) {
        viewModelScope.launch {
            routineRepository.permanentlyDeleteRoutine(routineId)
        }
    }

    fun updateRoutine(routine: Routine, steps: List<RoutineStep>) {
        viewModelScope.launch {
            val indexedSteps = steps.mapIndexed { index, step ->
                step.copy(orderIndex = index)
            }
            saveRoutineUseCase(routine, indexedSteps)
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            deleteScheduleUseCase(scheduleId)
        }
    }

    fun duplicateRoutine(routineWithDetails: RoutineWithDetails) {
        viewModelScope.launch {
            val original = routineWithDetails.routine
            val duplicatedRoutine = Routine(
                id = UUID.randomUUID().toString(),
                name = "${original.name} (Copy)",
                description = original.description,
                icon = original.icon
            )
            val duplicatedSteps = routineWithDetails.steps.map { step ->
                step.copy(
                    id = UUID.randomUUID().toString(),
                    routineId = duplicatedRoutine.id
                )
            }
            saveRoutineUseCase(duplicatedRoutine, duplicatedSteps)
        }
    }

    fun createStarterTemplate(name: String, icon: String, steps: List<Pair<String, Int>>) {
        viewModelScope.launch {
            val routine = Routine(
                id = UUID.randomUUID().toString(),
                name = name,
                icon = icon
            )
            val stepList = steps.mapIndexed { index, (stepName, mins) ->
                RoutineStep(
                    routineId = routine.id,
                    name = stepName,
                    orderIndex = index,
                    durationMinutes = mins,
                    durationSeconds = 0,
                    isForcefulAlarm = (index == 0) // First step forceful alarm, subsequent steps chime
                )
            }
            saveRoutineUseCase(routine, stepList)
        }
    }
}
