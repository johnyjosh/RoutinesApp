package com.excalibur.routines.presentation.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.excalibur.routines.alarm.ActiveRoutineNotificationManager
import com.excalibur.routines.alarm.AlarmSoundManager
import com.excalibur.routines.alarm.VoiceAnnouncer
import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.model.UserStats
import com.excalibur.routines.domain.repository.RoutineRepository
import com.excalibur.routines.domain.repository.UserStatsRepository
import com.excalibur.routines.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ActiveSessionUiState(
    val routine: Routine? = null,
    val steps: List<RoutineStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val remainingSecondsInStep: Int = 0,
    val totalSecondsInStep: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val keepScreenOn: Boolean = true,
    val voiceCuesEnabled: Boolean = true,
    val isBonusLounge: Boolean = false,
    val activeGameType: MiniGameType = MiniGameType.BERRY_CATCH,
    val sessionPoints: Int = 0,
    val userStats: UserStats? = null,
    val feedbackMessage: String? = null
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

    val formattedEstimatedFinishTime: String
        get() {
            val remainingSecs = remainingSecondsInStep + steps.drop(currentStepIndex + 1).sumOf { it.totalSeconds }
            val finishTime = LocalTime.now().plusSeconds(remainingSecs.toLong())
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            return finishTime.format(formatter)
        }
}

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val userStatsRepository: UserStatsRepository,
    private val alarmSoundManager: AlarmSoundManager,
    private val voiceAnnouncer: VoiceAnnouncer,
    private val activeRoutineNotificationManager: ActiveRoutineNotificationManager
) : ViewModel() {

    private val route: Screen.ActiveSession = savedStateHandle.toRoute()
    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            userStatsRepository.getUserStats().collect { stats ->
                _uiState.update { it.copy(userStats = stats) }
            }
        }

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
                announceStep(firstStep, isFirst = true)
                startTimer()
            }
        }
    }

    fun addSessionPoints(points: Int, reason: String) {
        if (points <= 0) return
        _uiState.update {
            it.copy(
                sessionPoints = it.sessionPoints + points,
                feedbackMessage = "+$points pts • $reason"
            )
        }
        viewModelScope.launch {
            userStatsRepository.addPoints(points)
        }
    }

    fun addTimeToCurrentStep(seconds: Int) {
        if (seconds <= 0) return
        _uiState.update { current ->
            val newRemaining = current.remainingSecondsInStep + seconds
            val newTotal = maxOf(current.totalSecondsInStep, newRemaining)
            current.copy(
                remainingSecondsInStep = newRemaining,
                totalSecondsInStep = newTotal,
                feedbackMessage = "+${seconds / 60}m extended ⏱️"
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    private fun announceStep(step: RoutineStep, isFirst: Boolean = false) {
        if (!_uiState.value.voiceCuesEnabled) return
        val prefix = if (isFirst) "Starting" else "Next step"
        val durationStr = if (step.durationMinutes > 0) "${step.durationMinutes} minutes" else "${step.totalSeconds} seconds"
        voiceAnnouncer.speak("$prefix: ${step.name}, $durationStr")
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value
                if (!current.isPaused && !current.isFinished) {
                    if (current.remainingSecondsInStep > 1) {
                        val nextRemaining = current.remainingSecondsInStep - 1
                        _uiState.update { it.copy(remainingSecondsInStep = nextRemaining) }

                        val updated = _uiState.value
                        activeRoutineNotificationManager.showOrUpdateNotification(
                            routineName = updated.routine?.name ?: "Routine",
                            stepName = updated.currentStep?.name ?: "Step",
                            stepIndex = updated.currentStepIndex,
                            totalSteps = updated.steps.size,
                            formattedRemainingTime = updated.formattedRemainingTime,
                            isPaused = updated.isPaused,
                            isBonusLounge = updated.isBonusLounge
                        )

                        // Hands-free mid-step voice cues
                        if (current.voiceCuesEnabled) {
                            val total = current.totalSecondsInStep
                            if (total >= 60 && nextRemaining == total / 2) {
                                voiceAnnouncer.speak("Halfway through ${current.currentStep?.name ?: "this step"}")
                            } else if (total >= 60 && nextRemaining == 30) {
                                voiceAnnouncer.speak("30 seconds remaining")
                            } else if (nextRemaining == 10) {
                                voiceAnnouncer.speak("10 seconds")
                            }
                        }
                    } else {
                        // Step completed! Award points and advance
                        advanceToNextStep(earnedStepPoints = true)
                    }
                }
            }
        }
    }

    private fun advanceToNextStep(earnedStepPoints: Boolean = true) {
        val current = _uiState.value
        val nextIndex = current.currentStepIndex + 1

        if (earnedStepPoints) {
            addSessionPoints(10, "Step Complete! 🎯")
        }

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
                    totalSecondsInStep = nextStep.totalSeconds,
                    isBonusLounge = false
                )
            }
            announceStep(nextStep, isFirst = false)
        } else {
            // Entire routine finished! Award 100 completion points and record daily streak
            timerJob?.cancel()
            activeRoutineNotificationManager.dismissNotification()
            viewModelScope.launch {
                val updatedStats = userStatsRepository.recordRoutineCompletion(100)
                _uiState.update {
                    it.copy(
                        isFinished = true,
                        remainingSecondsInStep = 0,
                        isBonusLounge = false,
                        sessionPoints = it.sessionPoints + 100,
                        userStats = updatedStats,
                        feedbackMessage = "+100 pts • Routine Complete! 🏆"
                    )
                }
                if (current.voiceCuesEnabled) {
                    voiceAnnouncer.speak("Routine complete! Great job!")
                }
            }
        }
    }

    fun enterBonusLounge() {
        addSessionPoints(50, "Early Bird Bonus! ⚡")
        val currentPoints = (_uiState.value.userStats?.totalPoints ?: 0) + 50
        val unlockedGames = MiniGameType.entries.filter { it.isUnlocked(currentPoints) }
        val chosenGame = unlockedGames.randomOrNull() ?: MiniGameType.BERRY_CATCH

        // Cap the bonus gap to max 45 seconds so it is a quick gap break, not endless gaming
        val currentRemaining = _uiState.value.remainingSecondsInStep
        val cappedGapSeconds = minOf(currentRemaining, 45).coerceAtLeast(10)

        _uiState.update {
            it.copy(
                isBonusLounge = true,
                activeGameType = chosenGame,
                remainingSecondsInStep = cappedGapSeconds,
                totalSecondsInStep = cappedGapSeconds
            )
        }
        if (_uiState.value.voiceCuesEnabled) {
            voiceAnnouncer.speak("You finished early! 45-second bonus round.")
        }
    }

    fun switchGame() {
        _uiState.update { current ->
            val userPoints = current.userStats?.totalPoints ?: 0
            val unlockedGames = MiniGameType.entries.filter { it.isUnlocked(userPoints) }
            if (unlockedGames.isEmpty()) return@update current
            val currentIndex = unlockedGames.indexOf(current.activeGameType)
            val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % unlockedGames.size
            current.copy(activeGameType = unlockedGames[nextIndex])
        }
    }

    fun selectGame(game: MiniGameType) {
        _uiState.update { it.copy(activeGameType = game) }
    }

    fun exitBonusLoungeAndAdvance() {
        advanceToNextStep(earnedStepPoints = false)
    }

    fun togglePauseResume() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun skipStep() {
        advanceToNextStep(earnedStepPoints = false)
    }

    fun toggleKeepScreenOn() {
        _uiState.update { current ->
            val next = !current.keepScreenOn
            val msg = if (next) "💡 Keep Screen Awake ON" else "🌙 Keep Screen Awake OFF"
            current.copy(keepScreenOn = next, feedbackMessage = msg)
        }
    }

    fun toggleVoiceCues() {
        _uiState.update { current ->
            val next = !current.voiceCuesEnabled
            val msg = if (next) "🔊 Voice cues ON" else "🔇 Voice cues OFF"
            if (!next) {
                voiceAnnouncer.stop()
            }
            current.copy(voiceCuesEnabled = next, feedbackMessage = msg)
        }
        if (_uiState.value.voiceCuesEnabled) {
            voiceAnnouncer.speak("Voice announcements enabled")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        voiceAnnouncer.stop()
        alarmSoundManager.stopAlarm()
        activeRoutineNotificationManager.dismissNotification()
    }
}
