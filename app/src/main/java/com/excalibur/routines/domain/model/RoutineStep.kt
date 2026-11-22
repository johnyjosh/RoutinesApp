package com.excalibur.routines.domain.model

import java.util.UUID

data class RoutineStep(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val name: String,
    val orderIndex: Int,
    val durationMinutes: Int = 0,
    val durationSeconds: Int = 0,
    val isForcefulAlarm: Boolean = true
) {
    val totalSeconds: Int
        get() = (durationMinutes * 60) + durationSeconds

    val formattedDuration: String
        get() {
            val totalMins = totalSeconds / 60
            val secs = totalSeconds % 60
            val hours = totalMins / 60
            val remainingMins = totalMins % 60

            return when {
                hours > 0 && remainingMins > 0 -> "${hours}h ${remainingMins}m"
                hours > 0 -> "${hours}h"
                remainingMins > 0 && secs > 0 -> "${remainingMins}m ${secs}s"
                remainingMins > 0 -> "${remainingMins}m"
                else -> "${secs}s"
            }
        }
}
