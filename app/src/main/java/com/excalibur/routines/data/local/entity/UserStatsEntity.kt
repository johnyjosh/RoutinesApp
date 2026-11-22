package com.excalibur.routines.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedDate: String? = null,
    val routinesCompletedCount: Int = 0,
    val streakFreezesAvailable: Int = 1,
    val lastFreezeUsedDate: String? = null
)
