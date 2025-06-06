package com.excalibur.routines.domain.models

import java.time.Duration

data class TimeInterval(
    val id: String,
    val name: String,
    val duration: Duration
) {
    constructor(id: String, name: String, hours: Int, minutes: Int) : this(
        id = id,
        name = name,
        duration = Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())
    )
    
    fun getDurationInMinutes(): Long = duration.toMinutes()
    
    fun getHours(): Int = (duration.toHours() % 24).toInt()
    
    fun getMinutes(): Int = (duration.toMinutes() % 60).toInt()
    
    fun getDisplayString(): String {
        val hours = getHours()
        val minutes = getMinutes()
        
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
    
    fun getTotalDurationString(): String {
        val totalMinutes = getDurationInMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}