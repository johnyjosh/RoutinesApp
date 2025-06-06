package com.excalibur.routines.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.TimeInterval
import com.excalibur.routines.domain.repositories.RoutineRepository
import com.excalibur.routines.domain.repositories.RoutineInstanceRepository
import com.excalibur.routines.domain.services.RoutineAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class RoutineViewModel(
    private val routineRepository: RoutineRepository,
    private val routineInstanceRepository: RoutineInstanceRepository,
    private val routineAlarmScheduler: RoutineAlarmScheduler
) : ViewModel() {

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _editingRoutine = MutableStateFlow<Routine?>(null)
    val editingRoutine: StateFlow<Routine?> = _editingRoutine.asStateFlow()

    init {
        loadRoutines()
    }

    fun loadRoutines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _routines.value = routineRepository.getAllRoutines()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load routines: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
        _editingRoutine.value = null
    }

    fun startEditingRoutine(routine: Routine) {
        _editingRoutine.value = routine
        _showCreateDialog.value = true
    }

    fun createRoutine(name: String, timeIntervals: List<TimeInterval>) {
        if (name.isBlank()) {
            _errorMessage.value = "Routine name cannot be empty"
            return
        }

        if (timeIntervals.isEmpty()) {
            _errorMessage.value = "Routine must have at least one time interval"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val routine = Routine.create(name, timeIntervals)
                routineRepository.createRoutine(routine).fold(
                    onSuccess = {
                        loadRoutines()
                        hideCreateDialog()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to create routine: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create routine: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRoutine(routine: Routine, name: String, timeIntervals: List<TimeInterval>) {
        if (name.isBlank()) {
            _errorMessage.value = "Routine name cannot be empty"
            return
        }

        if (timeIntervals.isEmpty()) {
            _errorMessage.value = "Routine must have at least one time interval"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedRoutine = routine.updateWith(name = name, timeIntervals = timeIntervals)
                routineRepository.updateRoutine(updatedRoutine).fold(
                    onSuccess = {
                        // Reschedule alarms for all instances of this routine
                        rescheduleAlarmsForRoutine(updatedRoutine)
                        loadRoutines()
                        hideCreateDialog()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to update routine: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update routine: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cancel alarms for all instances of this routine
                val instances = routineInstanceRepository.getRoutineInstancesForRoutine(routine.id)
                instances.forEach { instance ->
                    routineAlarmScheduler.cancelRoutineInstance(instance)
                }
                
                // Delete all routine instances for this routine first
                routineInstanceRepository.deleteRoutineInstancesForRoutine(routine.id)
                
                routineRepository.deleteRoutine(routine.id).fold(
                    onSuccess = {
                        loadRoutines()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to delete routine: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete routine: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun duplicateRoutine(routine: Routine, newName: String) {
        if (newName.isBlank()) {
            _errorMessage.value = "New routine name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                routineRepository.duplicateRoutine(routine.id, newName).fold(
                    onSuccess = {
                        loadRoutines()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to duplicate routine: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to duplicate routine: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun rescheduleAlarmsForRoutine(routine: Routine) {
        viewModelScope.launch {
            try {
                val instances = routineInstanceRepository.getRoutineInstancesForRoutine(routine.id)
                instances.forEach { instance ->
                    routineAlarmScheduler.scheduleRoutineInstance(instance, routine)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reschedule alarms: ${e.message}"
            }
        }
    }

    class Factory(
        private val routineRepository: RoutineRepository,
        private val routineInstanceRepository: RoutineInstanceRepository,
        private val routineAlarmScheduler: RoutineAlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RoutineViewModel::class.java)) {
                return RoutineViewModel(routineRepository, routineInstanceRepository, routineAlarmScheduler) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}