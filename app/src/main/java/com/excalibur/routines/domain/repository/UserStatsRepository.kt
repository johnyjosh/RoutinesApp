package com.excalibur.routines.domain.repository

import com.excalibur.routines.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface UserStatsRepository {
    fun getUserStats(): Flow<UserStats>
    suspend fun getUserStatsOnce(): UserStats
    suspend fun addPoints(amount: Int)
    suspend fun recordRoutineCompletion(routinePoints: Int): UserStats
}
