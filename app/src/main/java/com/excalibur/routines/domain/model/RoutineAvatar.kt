package com.excalibur.routines.domain.model

enum class RoutineAvatar(val emoji: String, val displayName: String) {
    FOX("🦊", "Fox"),
    BEAR("🐻", "Bear"),
    CAT("🐱", "Cat"),
    DOG("🐶", "Dog"),
    LION("🦁", "Lion"),
    RABBIT("🐰", "Rabbit"),
    OWL("🦉", "Owl"),
    PANDA("🐼", "Panda"),
    TURTLE("🐢", "Turtle"),
    KOALA("🐨", "Koala");

    companion object {
        fun fromEmoji(emoji: String): RoutineAvatar {
            return entries.firstOrNull { it.emoji == emoji } ?: FOX
        }
    }
}
