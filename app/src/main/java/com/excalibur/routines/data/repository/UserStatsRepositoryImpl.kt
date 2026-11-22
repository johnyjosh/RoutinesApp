package com.excalibur.routines.data.repository

import com.excalibur.routines.data.local.dao.UserStatsDao
import com.excalibur.routines.data.local.entity.UserStatsEntity
import com.excalibur.routines.domain.model.UserStats
import com.excalibur.routines.domain.repository.UserStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStatsRepositoryImpl @Inject constructor(
    private val userStatsDao: UserStatsDao
) : UserStatsRepository {

    override fun getUserStats(): Flow<UserStats> {
        return userStatsDao.getUserStats().map { entity ->
            val existing = entity ?: UserStatsEntity()
            // Weekly Streak Freeze replenishment: automatically restore to 1 if 7+ days have elapsed since last use
            if (existing.streakFreezesAvailable == 0 && existing.lastFreezeUsedDate != null) {
                try {
                    val lastUsed = LocalDate.parse(existing.lastFreezeUsedDate)
                    if (LocalDate.now().isAfter(lastUsed.plusDays(6))) {
                        val refreshed = existing.copy(streakFreezesAvailable = 1)
                        userStatsDao.upsertUserStats(refreshed)
                        return@map refreshed.toDomain()
                    }
                } catch (_: Exception) {}
            }
            existing.toDomain()
        }
    }

    override suspend fun getUserStatsOnce(): UserStats {
        val existing = userStatsDao.getUserStatsOnce() ?: UserStatsEntity()
        if (existing.streakFreezesAvailable == 0 && existing.lastFreezeUsedDate != null) {
            try {
                val lastUsed = LocalDate.parse(existing.lastFreezeUsedDate)
                if (LocalDate.now().isAfter(lastUsed.plusDays(6))) {
                    val refreshed = existing.copy(streakFreezesAvailable = 1)
                    userStatsDao.upsertUserStats(refreshed)
                    return refreshed.toDomain()
                }
            } catch (_: Exception) {}
        }
        return existing.toDomain()
    }

    override suspend fun addPoints(amount: Int) {
        val existing = userStatsDao.getUserStatsOnce() ?: UserStatsEntity()
        val updated = existing.copy(totalPoints = (existing.totalPoints + amount).coerceAtLeast(0))
        userStatsDao.upsertUserStats(updated)
    }

    override suspend fun recordRoutineCompletion(routinePoints: Int): UserStats {
        val existing = userStatsDao.getUserStatsOnce() ?: UserStatsEntity()
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val twoDaysAgo = LocalDate.now().minusDays(2).toString()

        var newStreakFreezes = existing.streakFreezesAvailable
        var newLastFreezeDate = existing.lastFreezeUsedDate

        val newStreak = when (existing.lastCompletedDate) {
            today -> existing.currentStreak.coerceAtLeast(1)
            yesterday -> existing.currentStreak + 1
            twoDaysAgo -> {
                if (existing.streakFreezesAvailable > 0) {
                    newStreakFreezes = (existing.streakFreezesAvailable - 1).coerceAtLeast(0)
                    newLastFreezeDate = today
                    existing.currentStreak + 1 // Saved by Streak Freeze!
                } else {
                    1
                }
            }
            else -> 1
        }

        val newBestStreak = maxOf(existing.bestStreak, newStreak)
        val newPoints = (existing.totalPoints + routinePoints).coerceAtLeast(0)
        val newCompletedCount = existing.routinesCompletedCount + 1

        // Grant 1 streak freeze every 7 completed routines, capped at 2
        if (newCompletedCount % 7 == 0 && newStreakFreezes < 2) {
            newStreakFreezes++
        }

        val updatedEntity = existing.copy(
            totalPoints = newPoints,
            currentStreak = newStreak,
            bestStreak = newBestStreak,
            lastCompletedDate = today,
            routinesCompletedCount = newCompletedCount,
            streakFreezesAvailable = newStreakFreezes,
            lastFreezeUsedDate = newLastFreezeDate
        )

        userStatsDao.upsertUserStats(updatedEntity)
        return updatedEntity.toDomain()
    }

    private fun UserStatsEntity.toDomain(): UserStats {
        return UserStats(
            totalPoints = totalPoints,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            lastCompletedDate = lastCompletedDate,
            routinesCompletedCount = routinesCompletedCount,
            streakFreezesAvailable = streakFreezesAvailable,
            lastFreezeUsedDate = lastFreezeUsedDate
        )
    }
}
