package com.excalibur.routines.domain.models

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class AlarmItem(
    val id: String,
    val time: LocalTime,
    val title: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val isRepeating: Boolean = true
) {
    fun getTimeString(): String {
        return time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
}