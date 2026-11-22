package com.excalibur.routines.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.excalibur.routines.domain.model.RoutineWithDetails

data class RoutineWithDetailsRelation(
    @Embedded val routine: RoutineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val steps: List<RoutineStepEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val schedules: List<RoutineScheduleEntity>
) {
    fun toDomain() = RoutineWithDetails(
        routine = routine.toDomain(),
        steps = steps.sortedBy { it.orderIndex }.map { it.toDomain() },
        schedules = schedules.sortedBy { it.createdAt }.map { it.toDomain() }
    )
}
