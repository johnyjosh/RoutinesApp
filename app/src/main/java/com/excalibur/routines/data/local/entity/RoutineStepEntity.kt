package com.excalibur.routines.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.excalibur.routines.domain.model.RoutineStep

@Entity(
    tableName = "routine_steps",
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
data class RoutineStepEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val name: String,
    val orderIndex: Int,
    val durationMinutes: Int,
    val durationSeconds: Int,
    val isForcefulAlarm: Boolean = true
) {
    fun toDomain() = RoutineStep(
        id = id,
        routineId = routineId,
        name = name,
        orderIndex = orderIndex,
        durationMinutes = durationMinutes,
        durationSeconds = durationSeconds,
        isForcefulAlarm = isForcefulAlarm
    )

    companion object {
        fun fromDomain(step: RoutineStep) = RoutineStepEntity(
            id = step.id,
            routineId = step.routineId,
            name = step.name,
            orderIndex = step.orderIndex,
            durationMinutes = step.durationMinutes,
            durationSeconds = step.durationSeconds,
            isForcefulAlarm = step.isForcefulAlarm
        )
    }
}
