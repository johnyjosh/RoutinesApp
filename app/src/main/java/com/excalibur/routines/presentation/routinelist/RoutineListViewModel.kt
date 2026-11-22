package com.excalibur.routines.presentation.routinelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineWithDetails
import com.excalibur.routines.domain.usecase.DeleteRoutineUseCase
import com.excalibur.routines.domain.usecase.DeleteScheduleUseCase
import com.excalibur.routines.domain.usecase.GetRoutinesUseCase
import com.excalibur.routines.domain.usecase.SaveRoutineUseCase
import com.excalibur.routines.domain.usecase.ToggleScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RoutineListUiState(
    val routines: List<RoutineWithDetails> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    getRoutinesUseCase: GetRoutinesUseCase,
    private val saveRoutineUseCase: SaveRoutineUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
    private val toggleScheduleUseCase: ToggleScheduleUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    val uiState: StateFlow<RoutineListUiState> = getRoutinesUseCase()
        .map { RoutineListUiState(routines = it, isLoading = false) }
        .stateIn(
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
                description = original.description
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
}
