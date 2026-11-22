package com.excalibur.routines.presentation.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameUnlockTest {

    @Test
    fun testBerryCatchIsAlwaysUnlocked() {
        assertTrue(MiniGameType.BERRY_CATCH.isUnlocked(0))
        assertTrue(MiniGameType.BERRY_CATCH.isUnlocked(50))
        assertTrue(MiniGameType.BERRY_CATCH.isUnlockedForLevel(1))
    }

    @Test
    fun testBrainSparkRequires100Points() {
        assertFalse(MiniGameType.MEMORY_MATCH.isUnlocked(99))
        assertTrue(MiniGameType.MEMORY_MATCH.isUnlocked(100))
        assertFalse(MiniGameType.MEMORY_MATCH.isUnlockedForLevel(1))
        assertTrue(MiniGameType.MEMORY_MATCH.isUnlockedForLevel(2))
    }

    @Test
    fun testProgressionUnlocksHigherTierGames() {
        val points50 = 50
        val unlockedAt50 = MiniGameType.entries.filter { it.isUnlocked(points50) }
        assertEquals(listOf(MiniGameType.BERRY_CATCH), unlockedAt50)

        val points350 = 350
        val unlockedAt350 = MiniGameType.entries.filter { it.isUnlocked(points350) }
        assertEquals(
            listOf(MiniGameType.BERRY_CATCH, MiniGameType.MEMORY_MATCH, MiniGameType.ZEN_BLOOM),
            unlockedAt350
        )

        val points2000 = 2000
        val unlockedAt2000 = MiniGameType.entries.filter { it.isUnlocked(points2000) }
        assertEquals(MiniGameType.entries.size, unlockedAt2000.size)
    }
}
