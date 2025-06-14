package com.excalibur.routines.data.repositories

import android.content.SharedPreferences
import android.util.Log
import com.excalibur.routines.domain.models.RoutineInstance
import com.excalibur.routines.domain.repositories.RoutineInstanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AndroidRoutineInstanceRepository(
    private val sharedPreferences: SharedPreferences
) : RoutineInstanceRepository {

    companion object {
        private const val TAG = "AndroidRoutineInstanceRepository"
        private const val ROUTINE_INSTANCES_KEY = "routine_instances"
        private const val FIELD_SEPARATOR = "|"
        private const val DAY_SEPARATOR = ","
    }

    override suspend fun createRoutineInstance(routineInstance: RoutineInstance): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val instances = getAllRoutineInstances().toMutableList()
            instances.add(routineInstance)
            saveRoutineInstancesToPreferences(instances)
            Log.d(TAG, "RoutineInstance created: ${routineInstance.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create routine instance", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllRoutineInstances(): List<RoutineInstance> = withContext(Dispatchers.IO) {
        return@withContext loadRoutineInstancesFromPreferences()
    }

    override suspend fun getRoutineInstance(instanceId: String): RoutineInstance? = withContext(Dispatchers.IO) {
        return@withContext loadRoutineInstancesFromPreferences().find { it.id == instanceId }
    }

    override suspend fun getRoutineInstancesForRoutine(routineId: String): List<RoutineInstance> = withContext(Dispatchers.IO) {
        return@withContext loadRoutineInstancesFromPreferences().filter { it.routineId == routineId }
    }

    override suspend fun updateRoutineInstance(routineInstance: RoutineInstance): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val instances = getAllRoutineInstances().toMutableList()
            val index = instances.indexOfFirst { it.id == routineInstance.id }
            if (index >= 0) {
                instances[index] = routineInstance
                saveRoutineInstancesToPreferences(instances)
                Log.d(TAG, "RoutineInstance updated: ${routineInstance.id}")
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("RoutineInstance not found: ${routineInstance.id}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update routine instance", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteRoutineInstance(instanceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val instances = getAllRoutineInstances().toMutableList()
            val removed = instances.removeAll { it.id == instanceId }
            if (removed) {
                saveRoutineInstancesToPreferences(instances)
                Log.d(TAG, "RoutineInstance deleted: $instanceId")
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("RoutineInstance not found: $instanceId"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete routine instance", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteRoutineInstancesForRoutine(routineId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val instances = getAllRoutineInstances().toMutableList()
            val removed = instances.removeAll { it.routineId == routineId }
            if (removed) {
                saveRoutineInstancesToPreferences(instances)
                Log.d(TAG, "RoutineInstances deleted for routine: $routineId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete routine instances for routine", e)
            Result.failure(e)
        }
    }

    private fun saveRoutineInstancesToPreferences(instances: List<RoutineInstance>) {
        val instanceStrings = instances.map { instance ->
            val daysString = instance.daysOfWeek.joinToString(DAY_SEPARATOR) { it.value.toString() }
            "${instance.id}${FIELD_SEPARATOR}${instance.routineId}${FIELD_SEPARATOR}${instance.name}${FIELD_SEPARATOR}${instance.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}${FIELD_SEPARATOR}$daysString${FIELD_SEPARATOR}${instance.isEnabled}${FIELD_SEPARATOR}${instance.createdAt}"
        }.toSet()

        sharedPreferences.edit()
            .putStringSet(ROUTINE_INSTANCES_KEY, instanceStrings)
            .apply()
    }

    private fun loadRoutineInstancesFromPreferences(): List<RoutineInstance> {
        val instanceStrings = sharedPreferences.getStringSet(ROUTINE_INSTANCES_KEY, emptySet()) ?: emptySet()
        return instanceStrings.mapNotNull { instanceString ->
            try {
                parseRoutineInstanceString(instanceString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse routine instance from preferences: $instanceString", e)
                null
            }
        }.sortedWith(compareBy<RoutineInstance> { it.startTime }.thenBy { it.daysOfWeek.minOrNull()?.value ?: 0 })
    }

    private fun parseRoutineInstanceString(instanceString: String): RoutineInstance {
        val parts = instanceString.split(FIELD_SEPARATOR)
        
        // Handle backward compatibility: if old format (6 fields), create default name
        if (parts.size == 6) {
            val id = parts[0]
            val routineId = parts[1]
            val startTime = LocalTime.parse(parts[2], DateTimeFormatter.ofPattern("HH:mm"))
            val daysOfWeek = if (parts[3].isNotEmpty()) {
                parts[3].split(DAY_SEPARATOR).map { DayOfWeek.of(it.toInt()) }.toSet()
            } else {
                emptySet()
            }
            val isEnabled = parts[4].toBoolean()
            val createdAt = parts[5].toLong()

            return RoutineInstance(
                id = id,
                routineId = routineId,
                name = "Schedule", // Default name for backward compatibility
                startTime = startTime,
                daysOfWeek = daysOfWeek,
                isEnabled = isEnabled,
                createdAt = createdAt
            )
        }
        
        // New format with name field (7 fields)
        if (parts.size < 7) throw IllegalArgumentException("Invalid routine instance format")

        val id = parts[0]
        val routineId = parts[1]
        val name = parts[2]
        val startTime = LocalTime.parse(parts[3], DateTimeFormatter.ofPattern("HH:mm"))
        val daysOfWeek = if (parts[4].isNotEmpty()) {
            parts[4].split(DAY_SEPARATOR).map { DayOfWeek.of(it.toInt()) }.toSet()
        } else {
            emptySet()
        }
        val isEnabled = parts[5].toBoolean()
        val createdAt = parts[6].toLong()

        return RoutineInstance(
            id = id,
            routineId = routineId,
            name = name,
            startTime = startTime,
            daysOfWeek = daysOfWeek,
            isEnabled = isEnabled,
            createdAt = createdAt
        )
    }
}