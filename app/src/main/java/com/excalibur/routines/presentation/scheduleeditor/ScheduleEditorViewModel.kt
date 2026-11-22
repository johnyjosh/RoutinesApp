package com.excalibur.routines.presentation.scheduleeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.excalibur.routines.domain.model.RoutineAvatar
import com.excalibur.routines.domain.model.RoutineSchedule
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.ScheduleRepository
import com.excalibur.routines.domain.usecase.SaveScheduleUseCase
import com.excalibur.routines.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class ScheduleEditorUiState(
    val routineId: String = "",
    val routineName: String = "",
    val avatar: RoutineAvatar = RoutineAvatar.FOX,
    val steps: List<RoutineStep> = emptyList(),
    val scheduleId: String = UUID.randomUUID().toString(),
    val label: String = "",
    val startHour: Int = LocalTime.now().hour,
    val startMinute: Int = LocalTime.now().minute,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val isNew: Boolean = true
)

@HiltViewModel
class ScheduleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val scheduleRepository: ScheduleRepository,
    private val saveScheduleUseCase: SaveScheduleUseCase
) : ViewModel() {

    private val route: Screen.ScheduleEditor = savedStateHandle.toRoute()
    private val _uiState = MutableStateFlow(
        ScheduleEditorUiState(routineId = route.routineId)
    )
    val uiState: StateFlow<ScheduleEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val routineWithDetails = routineRepository.getRoutineWithDetailsOnce(route.routineId)
            val routineName = routineWithDetails?.routine?.name ?: "Routine"
            val icon = routineWithDetails?.routine?.icon ?: "🦊"
            val avatar = RoutineAvatar.fromEmoji(icon)
            val steps = routineWithDetails?.steps?.sortedBy { it.orderIndex } ?: emptyList()

            val scheduleId = route.scheduleId
            if (scheduleId != null) {
                val existing = scheduleRepository.getScheduleById(scheduleId)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            routineName = routineName,
                            avatar = avatar,
                            steps = steps,
                            scheduleId = existing.id,
                            label = existing.label,
                            startHour = existing.startHour,
                            startMinute = existing.startMinute,
                            daysOfWeek = existing.daysOfWeek,
                            soundEnabled = existing.soundEnabled,
                            vibrateEnabled = existing.vibrateEnabled,
                            isNew = false
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    routineName = routineName,
                    avatar = avatar,
                    steps = steps
                )
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(startHour = hour, startMinute = minute) }
    }

    fun toggleDay(day: DayOfWeek) {
        _uiState.update { current ->
            val updated = if (current.daysOfWeek.contains(day)) {
                current.daysOfWeek - day
            } else {
                current.daysOfWeek + day
            }
            current.copy(daysOfWeek = updated)
        }
    }

    fun setPresetDays(days: Set<DayOfWeek>) {
        _uiState.update { it.copy(daysOfWeek = days) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun setVibrateEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrateEnabled = enabled) }
    }

    fun save(onSuccess: () -> Unit) {
        val current = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val schedule = RoutineSchedule(
                id = current.scheduleId,
                routineId = current.routineId,
                label = current.label.ifBlank { current.routineName },
                startHour = current.startHour,
                startMinute = current.startMinute,
                daysOfWeek = current.daysOfWeek,
                isEnabled = true,
                soundEnabled = current.soundEnabled,
                vibrateEnabled = current.vibrateEnabled
            )
            saveScheduleUseCase(schedule)
            onSuccess()
        }
    }
}
