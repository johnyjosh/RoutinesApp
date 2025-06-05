package com.excalibur.routines.domain.repositories

import com.excalibur.routines.domain.models.AlarmItem

interface AlarmRepository {
    suspend fun scheduleAlarm(alarmItem: AlarmItem): Result<Unit>
    suspend fun scheduleTestAlarm(alarmItem: AlarmItem): Result<Unit>
    suspend fun cancelAlarm(alarmId: String): Result<Unit>
    suspend fun getAllAlarms(): List<AlarmItem>
    suspend fun getAlarm(alarmId: String): AlarmItem?
    suspend fun updateAlarm(alarmItem: AlarmItem): Result<Unit>
    suspend fun deleteAlarm(alarmId: String): Result<Unit>
}