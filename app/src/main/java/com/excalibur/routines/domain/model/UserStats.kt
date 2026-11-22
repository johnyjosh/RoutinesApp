package com.excalibur.routines.domain.model

data class UserStats(
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedDate: String? = null,
    val routinesCompletedCount: Int = 0
) {
    val level: Int
        get() = when {
            totalPoints < 100 -> 1
            totalPoints < 300 -> 2
            totalPoints < 600 -> 3
            totalPoints < 1000 -> 4
            totalPoints < 1500 -> 5
            totalPoints < 2500 -> 6
            else -> 7 + (totalPoints - 2500) / 1000
        }

    val levelTitle: String
        get() = when (level) {
            1 -> "Novice Starter"
            2 -> "Habit Builder"
            3 -> "Routine Master"
            4 -> "Focus Champion"
            5 -> "Zen Grandmaster"
            else -> "Legendary Achiever"
        }

    val currentLevelMinPoints: Int
        get() = when (level) {
            1 -> 0
            2 -> 100
            3 -> 300
            4 -> 600
            5 -> 1000
            6 -> 1500
            else -> 2500 + (level - 7) * 1000
        }

    val nextLevelThreshold: Int
        get() = when (level) {
            1 -> 100
            2 -> 300
            3 -> 600
            4 -> 1000
            5 -> 1500
            6 -> 2500
            else -> 2500 + (level - 6) * 1000
        }

    val levelProgressFraction: Float
        get() {
            val range = nextLevelThreshold - currentLevelMinPoints
            if (range <= 0) return 1f
            return ((totalPoints - currentLevelMinPoints).toFloat() / range.toFloat()).coerceIn(0f, 1f)
        }

    val pointsToNextLevel: Int
        get() = (nextLevelThreshold - totalPoints).coerceAtLeast(0)
}
