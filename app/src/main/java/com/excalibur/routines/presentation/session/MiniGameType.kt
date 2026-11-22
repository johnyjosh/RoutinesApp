package com.excalibur.routines.presentation.session

enum class MiniGameType(
    val title: String,
    val subtitle: String,
    val icon: String,
    val requiredLevel: Int,
    val requiredPoints: Int
) {
    BERRY_CATCH("Berry Catch", "Catch treats for your companion!", "🍓", requiredLevel = 1, requiredPoints = 0),
    MEMORY_MATCH("Brain Spark", "Match companion card pairs!", "🧩", requiredLevel = 2, requiredPoints = 100),
    ZEN_BLOOM("Zen Bloom", "Breathe & tap calming ripples", "🫧", requiredLevel = 3, requiredPoints = 300),
    CRITTER_POP("Critter Pop", "Tap cute critters before they hide!", "🐾", requiredLevel = 4, requiredPoints = 600),
    TOWER_STACK("Tower Stack", "Drop & stack blocks with precision!", "🧱", requiredLevel = 5, requiredPoints = 1000),
    CHROMA_ECHO("Chroma Echo", "Follow the glowing color pattern!", "🎨", requiredLevel = 6, requiredPoints = 1500);

    fun isUnlocked(totalPoints: Int): Boolean = totalPoints >= requiredPoints
    fun isUnlockedForLevel(level: Int): Boolean = level >= requiredLevel
}
