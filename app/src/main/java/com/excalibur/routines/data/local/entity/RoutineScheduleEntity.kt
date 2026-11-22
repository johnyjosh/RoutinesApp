package com.excalibur.routines.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.excalibur.routines.domain.model.RoutineSchedule
import java.time.DayOfWeek

@Entity(
    tableName = "routine_schedules",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId")]
)
data class RoutineScheduleEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val label: String,
    val startHour: Int,
    val startMinute: Int,
    val daysOfWeekCsv: String,
    val isEnabled: Boolean,
    val soundEnabled: Boolean,
    val vibrateEnabled: Boolean,
    val createdAt: Long
) {
    fun toDomain(): RoutineSchedule {
        val days = if (daysOfWeekCsv.isBlank()) {
            emptySet()
        } else {
            daysOfWeekCsv.split(",")
                .filter { it.isNotBlank() }
                .map { DayOfWeek.valueOf(it.trim()) }
                .toSet()
        }
        return RoutineSchedule(
            id = id,
            routineId = routineId,
            label = label,
            startHour = startHour,
            startMinute = startMinute,
            daysOfWeek = days,
            isEnabled = isEnabled,
            soundEnabled = soundEnabled,
            vibrateEnabled = vibrateEnabled,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(schedule: RoutineSchedule) = RoutineScheduleEntity(
            id = schedule.id,
            routineId = schedule.routineId,
            label = schedule.label,
            startHour = schedule.startHour,
            startMinute = schedule.startMinute,
            daysOfWeekCsv = schedule.daysOfWeek.joinToString(",") { it.name },
            isEnabled = schedule.isEnabled,
            soundEnabled = schedule.soundEnabled,
            vibrateEnabled = schedule.vibrateEnabled,
            createdAt = schedule.createdAt
        )
    }
}
