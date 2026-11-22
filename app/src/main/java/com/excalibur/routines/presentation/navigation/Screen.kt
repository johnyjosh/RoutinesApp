package com.excalibur.routines.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object RoutineList : Screen

    @Serializable
    data class RoutineEditor(val routineId: String? = null) : Screen

    @Serializable
    data class ScheduleEditor(val routineId: String, val scheduleId: String? = null) : Screen
}
