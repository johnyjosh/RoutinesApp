package com.excalibur.routines.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class RoutineSchedule(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val label: String,
    val startHour: Int,
    val startMinute: Int,
    val daysOfWeek: Set<DayOfWeek>,
    val isEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val localTime: LocalTime
        get() = LocalTime.of(startHour, startMinute)

    val formattedTime: String
        get() = localTime.format(DateTimeFormatter.ofPattern("h:mm a"))

    val recurrenceText: String
        get() {
            if (daysOfWeek.isEmpty()) return "One-time"
            val weekdays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
            val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val allDays = DayOfWeek.entries.toSet()

            return when {
                daysOfWeek == allDays -> "Every day"
                daysOfWeek == weekdays -> "Weekdays"
                daysOfWeek == weekends -> "Weekends"
                daysOfWeek.size == 1 -> daysOfWeek.first().name.lowercase().replaceFirstChar { it.uppercase() }
                else -> daysOfWeek.sortedBy { it.value }.joinToString(", ") {
                    it.name.take(3).lowercase().replaceFirstChar { char -> char.uppercase() }
                }
            }
        }
}
