package com.excalibur.routines.presentation.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.excalibur.routines.alarm.AlarmSoundManager
import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveSessionUiState(
    val routine: Routine? = null,
    val steps: List<RoutineStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val remainingSecondsInStep: Int = 0,
    val totalSecondsInStep: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val keepScreenOn: Boolean = true
) {
    val currentStep: RoutineStep?
        get() = steps.getOrNull(currentStepIndex)

    val progressFraction: Float
        get() {
            if (totalSecondsInStep <= 0) return 0f
            val elapsed = totalSecondsInStep - remainingSecondsInStep
            return (elapsed.toFloat() / totalSecondsInStep.toFloat()).coerceIn(0f, 1f)
        }

    val formattedRemainingTime: String
        get() {
            val mins = remainingSecondsInStep / 60
            val secs = remainingSecondsInStep % 60
            return "%02d:%02d".format(mins, secs)
        }
}

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val alarmSoundManager: AlarmSoundManager
) : ViewModel() {

    private val route: Screen.ActiveSession = savedStateHandle.toRoute()
    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val routineWithDetails = routineRepository.getRoutineWithDetailsOnce(route.routineId)
            if (routineWithDetails != null && routineWithDetails.steps.isNotEmpty()) {
                val firstStep = routineWithDetails.steps.first()
                _uiState.update {
                    it.copy(
                        routine = routineWithDetails.routine,
                        steps = routineWithDetails.steps,
                        currentStepIndex = 0,
                        remainingSecondsInStep = firstStep.totalSeconds,
                        totalSecondsInStep = firstStep.totalSeconds
                    )
                }
                startTimer()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value
                if (!current.isPaused && !current.isFinished) {
                    if (current.remainingSecondsInStep > 1) {
                        _uiState.update { it.copy(remainingSecondsInStep = it.remainingSecondsInStep - 1) }
                    } else {
                        // Step completed! Transition to next step or finish
                        advanceToNextStep()
                    }
                }
            }
        }
    }

    private fun advanceToNextStep() {
        val current = _uiState.value
        val nextIndex = current.currentStepIndex + 1

        // Trigger gentle chime or sound for step transition
        viewModelScope.launch {
            alarmSoundManager.startAlarm(soundEnabled = true, vibrateEnabled = true)
            delay(2500L)
            alarmSoundManager.stopAlarm()
        }

        if (nextIndex < current.steps.size) {
            val nextStep = current.steps[nextIndex]
            _uiState.update {
                it.copy(
                    currentStepIndex = nextIndex,
                    remainingSecondsInStep = nextStep.totalSeconds,
                    totalSecondsInStep = nextStep.totalSeconds
                )
            }
        } else {
            // Entire routine finished!
            _uiState.update { it.copy(isFinished = true, remainingSecondsInStep = 0) }
            timerJob?.cancel()
        }
    }

    fun togglePauseResume() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun skipStep() {
        advanceToNextStep()
    }

    fun toggleKeepScreenOn() {
        _uiState.update { it.copy(keepScreenOn = !it.keepScreenOn) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        alarmSoundManager.stopAlarm()
    }
}
