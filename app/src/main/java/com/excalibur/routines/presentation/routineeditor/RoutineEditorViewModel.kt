package com.excalibur.routines.presentation.routineeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.usecase.SaveRoutineUseCase
import com.excalibur.routines.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RoutineEditorUiState(
    val routineId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val steps: List<RoutineStep> = emptyList(),
    val isSaving: Boolean = false,
    val isNew: Boolean = true
)

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val saveRoutineUseCase: SaveRoutineUseCase
) : ViewModel() {

    private val route: Screen.RoutineEditor = savedStateHandle.toRoute()
    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    init {
        val routineId = route.routineId
        if (routineId != null) {
            viewModelScope.launch {
                val existing = routineRepository.getRoutineWithDetailsOnce(routineId)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            routineId = existing.routine.id,
                            name = existing.routine.name,
                            description = existing.routine.description,
                            steps = existing.steps,
                            isNew = false
                        )
                    }
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun addStep(name: String, minutes: Int, seconds: Int) {
        _uiState.update { current ->
            val newStep = RoutineStep(
                routineId = current.routineId,
                name = name.ifBlank { "Step ${current.steps.size + 1}" },
                orderIndex = current.steps.size,
                durationMinutes = minutes,
                durationSeconds = seconds
            )
            current.copy(steps = current.steps + newStep)
        }
    }

    fun removeStep(stepId: String) {
        _uiState.update { current ->
            val updated = current.steps.filterNot { it.id == stepId }
                .mapIndexed { index, step -> step.copy(orderIndex = index) }
            current.copy(steps = updated)
        }
    }

    fun moveStep(fromIndex: Int, toIndex: Int) {
        _uiState.update { current ->
            if (fromIndex !in current.steps.indices || toIndex !in current.steps.indices) {
                return@update current
            }
            val mutableList = current.steps.toMutableList()
            val item = mutableList.removeAt(fromIndex)
            mutableList.add(toIndex, item)
            val updated = mutableList.mapIndexed { index, step -> step.copy(orderIndex = index) }
            current.copy(steps = updated)
        }
    }

    fun save(onSuccess: () -> Unit) {
        val current = _uiState.value
        if (current.name.isBlank()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val routine = Routine(
                id = current.routineId,
                name = current.name.trim(),
                description = current.description.trim()
            )
            saveRoutineUseCase(routine, current.steps)
            onSuccess()
        }
    }
}
