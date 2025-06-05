package com.excalibur.routines.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.excalibur.routines.data.managers.PermissionManager
import com.excalibur.routines.domain.models.AlarmItem
import com.excalibur.routines.domain.repositories.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

class AlarmViewModel(
    private val alarmRepository: AlarmRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _showTimePicker = MutableStateFlow(false)
    val showTimePicker: StateFlow<Boolean> = _showTimePicker.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAlarms()
    }

    fun showTimePickerForNewAlarm() {
        _showTimePicker.value = true
    }

    fun hideTimePicker() {
        _showTimePicker.value = false
    }

    fun onTimeSelectedAndAddAlarm(hour: Int, minute: Int) {
        hideTimePicker()
        addAlarmDirectly(hour, minute)
    }

    private fun addAlarmDirectly(hour: Int, minute: Int) {
        val time = LocalTime.of(hour, minute)

        if (_alarms.value.any { it.time == time }) {
            _errorMessage.value = "Alarm already exists for this time"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val alarmItem = AlarmItem(
                    id = UUID.randomUUID().toString(),
                    time = time,
                    title = "Routine Time",
                    isEnabled = true,
                    isRepeating = true
                )

                alarmRepository.scheduleAlarm(alarmItem).fold(
                    onSuccess = {
                        loadAlarms()
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to schedule alarm: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create alarm: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeAlarm(alarmItem: AlarmItem) {
        viewModelScope.launch {
            _isLoading.value = true
            alarmRepository.deleteAlarm(alarmItem.id).fold(
                onSuccess = {
                    loadAlarms()
                },
                onFailure = { error ->
                    _errorMessage.value = "Failed to remove alarm: ${error.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun toggleAlarm(alarmItem: AlarmItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val updatedAlarm = alarmItem.copy(isEnabled = !alarmItem.isEnabled)
            alarmRepository.updateAlarm(updatedAlarm).fold(
                onSuccess = {
                    loadAlarms()
                },
                onFailure = { error ->
                    _errorMessage.value = "Failed to update alarm: ${error.message}"
                }
            )
            _isLoading.value = false
        }
    }

    fun testAlarms() {
        viewModelScope.launch {
            _alarms.value.filter { it.isEnabled }.forEach { alarm ->
                alarmRepository.scheduleTestAlarm(
                    alarm.copy(
                        id = "${alarm.id}_test_${System.currentTimeMillis()}",
                        time = LocalTime.now().plusSeconds(2)
                    )
                )
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun hasNotificationPermission(): Boolean {
        return permissionManager.hasNotificationPermission()
    }

    fun canScheduleExactAlarms(): Boolean {
        return permissionManager.canScheduleExactAlarms()
    }

    fun getNotificationPermission(): String {
        return permissionManager.getNotificationPermission()
    }

    private fun loadAlarms() {
        viewModelScope.launch {
            _alarms.value = alarmRepository.getAllAlarms()
        }
    }

    class Factory(
        private val alarmRepository: AlarmRepository,
        private val permissionManager: PermissionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
                return AlarmViewModel(alarmRepository, permissionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}