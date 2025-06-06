package com.excalibur.routines.data.repositories

import android.content.SharedPreferences
import android.util.Log
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.TimeInterval
import com.excalibur.routines.domain.repositories.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.UUID

class AndroidRoutineRepository(
    private val sharedPreferences: SharedPreferences
) : RoutineRepository {

    companion object {
        private const val TAG = "AndroidRoutineRepository"
        private const val ROUTINES_KEY = "routines"
        private const val ROUTINE_SEPARATOR = "|||"
        private const val INTERVAL_SEPARATOR = ":::"
        private const val FIELD_SEPARATOR = "|"
    }

    override suspend fun createRoutine(routine: Routine): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val routines = getAllRoutines().toMutableList()
            routines.add(routine)
            saveRoutinesToPreferences(routines)
            Log.d(TAG, "Routine created: ${routine.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create routine", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllRoutines(): List<Routine> = withContext(Dispatchers.IO) {
        return@withContext loadRoutinesFromPreferences()
    }

    override suspend fun getRoutine(routineId: String): Routine? = withContext(Dispatchers.IO) {
        return@withContext loadRoutinesFromPreferences().find { it.id == routineId }
    }

    override suspend fun updateRoutine(routine: Routine): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val routines = getAllRoutines().toMutableList()
            val index = routines.indexOfFirst { it.id == routine.id }
            if (index >= 0) {
                routines[index] = routine.copy(updatedAt = System.currentTimeMillis())
                saveRoutinesToPreferences(routines)
                Log.d(TAG, "Routine updated: ${routine.name}")
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Routine not found: ${routine.id}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update routine", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteRoutine(routineId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val routines = getAllRoutines().toMutableList()
            val removed = routines.removeAll { it.id == routineId }
            if (removed) {
                saveRoutinesToPreferences(routines)
                Log.d(TAG, "Routine deleted: $routineId")
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Routine not found: $routineId"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete routine", e)
            Result.failure(e)
        }
    }

    override suspend fun duplicateRoutine(routineId: String, newName: String): Result<Routine> = withContext(Dispatchers.IO) {
        try {
            val originalRoutine = getRoutine(routineId)
                ?: return@withContext Result.failure(NoSuchElementException("Routine not found: $routineId"))
            
            val duplicatedRoutine = originalRoutine.duplicate(newName)
            createRoutine(duplicatedRoutine)
            Log.d(TAG, "Routine duplicated: ${originalRoutine.name} -> ${duplicatedRoutine.name}")
            Result.success(duplicatedRoutine)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to duplicate routine", e)
            Result.failure(e)
        }
    }

    private fun saveRoutinesToPreferences(routines: List<Routine>) {
        val routineStrings = routines.map { routine ->
            val intervalsString = routine.timeIntervals.joinToString(INTERVAL_SEPARATOR) { interval ->
                "${interval.id}~${interval.name}~${interval.duration.toMinutes()}"
            }
            "${routine.id}${FIELD_SEPARATOR}${routine.name}${FIELD_SEPARATOR}$intervalsString${FIELD_SEPARATOR}${routine.createdAt}${FIELD_SEPARATOR}${routine.updatedAt}"
        }.toSet()

        sharedPreferences.edit()
            .putStringSet(ROUTINES_KEY, routineStrings)
            .apply()
    }

    private fun loadRoutinesFromPreferences(): List<Routine> {
        val routineStrings = sharedPreferences.getStringSet(ROUTINES_KEY, emptySet()) ?: emptySet()
        return routineStrings.mapNotNull { routineString ->
            try {
                parseRoutineString(routineString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse routine from preferences: $routineString", e)
                null
            }
        }.sortedBy { it.name }
    }

    private fun parseRoutineString(routineString: String): Routine {
        val parts = routineString.split(FIELD_SEPARATOR)
        if (parts.size < 5) throw IllegalArgumentException("Invalid routine format")

        val id = parts[0]
        val name = parts[1]
        val intervalsString = parts[2]
        val createdAt = parts[3].toLong()
        val updatedAt = parts[4].toLong()

        val timeIntervals = if (intervalsString.isNotEmpty()) {
            intervalsString.split(INTERVAL_SEPARATOR).map { intervalString ->
                val intervalParts = intervalString.split("~")
                if (intervalParts.size < 3) throw IllegalArgumentException("Invalid interval format")
                
                TimeInterval(
                    id = intervalParts[0],
                    name = intervalParts[1],
                    duration = Duration.ofMinutes(intervalParts[2].toLong())
                )
            }
        } else {
            emptyList()
        }

        return Routine(
            id = id,
            name = name,
            timeIntervals = timeIntervals,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}