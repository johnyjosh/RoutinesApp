package com.excalibur.routines.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.RoutineInstance
import com.excalibur.routines.domain.repositories.RoutineInstanceRepository
import com.excalibur.routines.domain.repositories.RoutineRepository
import com.excalibur.routines.domain.services.RoutineAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

data class RoutineInstanceWithRoutine(
    val instance: RoutineInstance,
    val routine: Routine?
)

class RoutineInstanceViewModel(
    private val routineInstanceRepository: RoutineInstanceRepository,
    private val routineRepository: RoutineRepository,
    private val routineAlarmScheduler: RoutineAlarmScheduler
) : ViewModel() {

    private val _routineInstances = MutableStateFlow<List<RoutineInstanceWithRoutine>>(emptyList())
    val routineInstances: StateFlow<List<RoutineInstanceWithRoutine>> = _routineInstances.asStateFlow()

    private val _availableRoutines = MutableStateFlow<List<Routine>>(emptyList())
    val availableRoutines: StateFlow<List<Routine>> = _availableRoutines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showCreateScreen = MutableStateFlow(false)
    val showCreateScreen: StateFlow<Boolean> = _showCreateScreen.asStateFlow()

    private val _editingInstance = MutableStateFlow<RoutineInstance?>(null)
    val editingInstance: StateFlow<RoutineInstance?> = _editingInstance.asStateFlow()
    
    private val _preSelectedRoutineId = MutableStateFlow<String?>(null)
    val preSelectedRoutineId: StateFlow<String?> = _preSelectedRoutineId.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val routines = routineRepository.getAllRoutines()
                val instances = routineInstanceRepository.getAllRoutineInstances()
                
                _availableRoutines.value = routines
                _routineInstances.value = instances.map { instance ->
                    RoutineInstanceWithRoutine(
                        instance = instance,
                        routine = routines.find { it.id == instance.routineId }
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showCreateScreen() {
        _preSelectedRoutineId.value = null
        _showCreateScreen.value = true
    }
    
    fun showCreateScreenWithRoutine(routineId: String) {
        _preSelectedRoutineId.value = routineId
        _showCreateScreen.value = true
    }

    fun hideCreateScreen() {
        _showCreateScreen.value = false
        _editingInstance.value = null
        _preSelectedRoutineId.value = null
    }

    fun startEditingInstance(instance: RoutineInstance) {
        _editingInstance.value = instance
        _showCreateScreen.value = true
    }

    fun createRoutineInstance(
        routineId: String,
        startTime: LocalTime,
        daysOfWeek: Set<DayOfWeek>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val instance = RoutineInstance.create(routineId, startTime, daysOfWeek)
                routineInstanceRepository.createRoutineInstance(instance).fold(
                    onSuccess = {
                        // Schedule alarms for the new routine instance
                        scheduleAlarmsForInstance(instance)
                        loadData()
                        hideCreateScreen()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to create routine instance: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create routine instance: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRoutineInstance(
        instance: RoutineInstance,
        routineId: String,
        startTime: LocalTime,
        daysOfWeek: Set<DayOfWeek>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedInstance = instance.copy(
                    routineId = routineId,
                    startTime = startTime,
                    daysOfWeek = daysOfWeek
                )
                routineInstanceRepository.updateRoutineInstance(updatedInstance).fold(
                    onSuccess = {
                        // Reschedule alarms for the updated routine instance
                        scheduleAlarmsForInstance(updatedInstance)
                        loadData()
                        hideCreateScreen()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to update routine instance: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update routine instance: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleRoutineInstance(instance: RoutineInstance) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updatedInstance = instance.copy(isEnabled = !instance.isEnabled)
                routineInstanceRepository.updateRoutineInstance(updatedInstance).fold(
                    onSuccess = {
                        // Reschedule or cancel alarms based on enabled state
                        scheduleAlarmsForInstance(updatedInstance)
                        loadData()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to toggle routine instance: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle routine instance: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRoutineInstance(instance: RoutineInstance) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cancel alarms before deleting the instance
                routineAlarmScheduler.cancelRoutineInstance(instance)
                
                routineInstanceRepository.deleteRoutineInstance(instance.id).fold(
                    onSuccess = {
                        loadData()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to delete routine instance: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete routine instance: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun refreshAvailableRoutines() {
        viewModelScope.launch {
            try {
                val routines = routineRepository.getAllRoutines()
                _availableRoutines.value = routines
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh routines: ${e.message}"
            }
        }
    }
    
    fun refreshRoutineInstancesAfterRoutineUpdate(updatedRoutineId: String) {
        viewModelScope.launch {
            try {
                val routines = routineRepository.getAllRoutines()
                val instances = routineInstanceRepository.getAllRoutineInstances()
                
                _availableRoutines.value = routines
                
                // Update routine instances with fresh routine data
                _routineInstances.value = instances.map { instance ->
                    RoutineInstanceWithRoutine(
                        instance = instance,
                        routine = routines.find { it.id == instance.routineId }
                    )
                }
                
                // Reschedule alarms for instances that use the updated routine
                val updatedRoutine = routines.find { it.id == updatedRoutineId }
                if (updatedRoutine != null) {
                    val affectedInstances = instances.filter { it.routineId == updatedRoutineId }
                    affectedInstances.forEach { instance ->
                        if (instance.isEnabled) {
                            routineAlarmScheduler.scheduleRoutineInstance(instance, updatedRoutine)
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh routine instances: ${e.message}"
            }
        }
    }

    private fun scheduleAlarmsForInstance(instance: RoutineInstance) {
        viewModelScope.launch {
            try {
                val routine = routineRepository.getRoutine(instance.routineId)
                if (routine != null) {
                    routineAlarmScheduler.scheduleRoutineInstance(instance, routine)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to schedule alarms: ${e.message}"
            }
        }
    }

    class Factory(
        private val routineInstanceRepository: RoutineInstanceRepository,
        private val routineRepository: RoutineRepository,
        private val routineAlarmScheduler: RoutineAlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RoutineInstanceViewModel::class.java)) {
                return RoutineInstanceViewModel(routineInstanceRepository, routineRepository, routineAlarmScheduler) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}