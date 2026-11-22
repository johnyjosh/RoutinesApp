package com.excalibur.routines.domain.model

data class RoutineWithDetails(
    val routine: Routine,
    val steps: List<RoutineStep> = emptyList(),
    val schedules: List<RoutineSchedule> = emptyList()
) {
    val totalDurationSeconds: Int
        get() = steps.sumOf { it.totalSeconds }

    val formattedTotalDuration: String
        get() {
            val totalMins = totalDurationSeconds / 60
            val hours = totalMins / 60
            val mins = totalMins % 60
            return when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
        }
}
