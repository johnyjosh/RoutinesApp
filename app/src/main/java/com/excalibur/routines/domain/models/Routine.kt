package com.excalibur.routines.domain.models

import java.time.Duration
import java.util.UUID

data class Routine(
    val id: String,
    val name: String,
    val timeIntervals: List<TimeInterval>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun create(name: String, timeIntervals: List<TimeInterval>): Routine {
            return Routine(
                id = UUID.randomUUID().toString(),
                name = name,
                timeIntervals = timeIntervals
            )
        }
    }
    
    fun getTotalDuration(): Duration {
        return timeIntervals.fold(Duration.ZERO) { acc, interval ->
            acc.plus(interval.duration)
        }
    }
    
    fun getTotalDurationString(): String {
        val totalDuration = getTotalDuration()
        val hours = totalDuration.toHours()
        val minutes = totalDuration.toMinutes() % 60
        
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
    
    fun duplicate(newName: String): Routine {
        return Routine(
            id = UUID.randomUUID().toString(),
            name = newName,
            timeIntervals = timeIntervals.map { interval ->
                interval.copy(id = UUID.randomUUID().toString())
            },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun updateWith(
        name: String? = null,
        timeIntervals: List<TimeInterval>? = null
    ): Routine {
        return copy(
            name = name ?: this.name,
            timeIntervals = timeIntervals ?: this.timeIntervals,
            updatedAt = System.currentTimeMillis()
        )
    }
}