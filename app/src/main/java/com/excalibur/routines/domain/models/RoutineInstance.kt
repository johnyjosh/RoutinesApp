package com.excalibur.routines.domain.models

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class RoutineInstance(
    val id: String,
    val routineId: String,
    val startTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun create(
            routineId: String,
            startTime: LocalTime,
            daysOfWeek: Set<DayOfWeek>
        ): RoutineInstance {
            return RoutineInstance(
                id = UUID.randomUUID().toString(),
                routineId = routineId,
                startTime = startTime,
                daysOfWeek = daysOfWeek
            )
        }
    }
    
    fun getStartTimeString(): String {
        return startTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    
    fun getDaysOfWeekString(): String {
        if (daysOfWeek.isEmpty()) return "Never"
        
        val sortedDays = daysOfWeek.sortedBy { it.value }
        
        // Check for common patterns
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val allDays = DayOfWeek.values().toSet()
        
        return when {
            daysOfWeek == allDays -> "Every day"
            daysOfWeek == weekdays -> "Weekdays"
            daysOfWeek == weekends -> "Weekends"
            daysOfWeek.size == 1 -> sortedDays.first().getDisplayName()
            else -> sortedDays.joinToString(", ") { it.getDisplayName() }
        }
    }
    
    fun getScheduleString(): String {
        return "${getStartTimeString()} • ${getDaysOfWeekString()}"
    }
    
    private fun DayOfWeek.getDisplayName(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
    }
}