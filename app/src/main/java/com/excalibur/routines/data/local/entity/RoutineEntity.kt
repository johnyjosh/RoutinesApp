package com.excalibur.routines.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.excalibur.routines.domain.model.Routine

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val icon: String = "🦊",
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = Routine(
        id = id,
        name = name,
        description = description,
        icon = icon,
        isArchived = isArchived,
        archivedAt = archivedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(routine: Routine) = RoutineEntity(
            id = routine.id,
            name = routine.name,
            description = routine.description,
            icon = routine.icon,
            isArchived = routine.isArchived,
            archivedAt = routine.archivedAt,
            createdAt = routine.createdAt,
            updatedAt = routine.updatedAt
        )
    }
}
