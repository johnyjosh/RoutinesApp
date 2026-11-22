package com.excalibur.routines.presentation.session.minigames

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

data class BurrowCritter(
    val id: Long = Random.nextLong(),
    val emoji: String,
    val isGolden: Boolean = false,
    val popTimeMillis: Long = System.currentTimeMillis()
)

@Composable
fun CritterPopGame(
    onScoreEarned: (points: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var score by remember { mutableIntStateOf(0) }
    var crittersPopped by remember { mutableIntStateOf(0) }
    var comboStreak by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    // 9 burrows (3x3 grid)
    val activeCritters = remember { mutableStateMapOf<Int, BurrowCritter>() }
    val critterEmojis = remember { listOf("🦊", "🐻", "🐱", "🐶", "🐼", "🐨", "🐰", "🦁") }

    // Game spawner loop
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random.nextLong(600L, 1000L))

            // Choose an empty burrow
            val availableBurrows = (0..8).filter { !activeCritters.containsKey(it) }
            if (availableBurrows.isNotEmpty()) {
                val burrowIndex = availableBurrows.random()
                val isGolden = Random.nextFloat() < 0.15f
                val emoji = if (isGolden) "⭐" else critterEmojis.random()
                val critter = BurrowCritter(
                    emoji = emoji,
                    isGolden = isGolden,
                    popTimeMillis = System.currentTimeMillis()
                )
                activeCritters[burrowIndex] = critter

                // Auto hide critter after duration
                val stayDuration = if (isGolden) 1100L else Random.nextLong(1200L, 1800L)
                delay(stayDuration)
                if (activeCritters[burrowIndex]?.id == critter.id) {
                    activeCritters.remove(burrowIndex)
                    // Reset combo if missed
                    comboStreak = 0
                }
            }
        }
    }

    LaunchedEffect(feedbackText) {
        if (feedbackText != null) {
            delay(900L)
            feedbackText = null
        }
    }

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Stats: Popped Count, Score, and Combo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🐾", fontSize = 16.sp)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "$crittersPopped Popped",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (comboStreak >= 3) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "🔥 ${comboStreak}x Combo!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "+$score pts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle & Feedback text
            Text(
                text = feedbackText ?: "Tap critters when they peek out!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (feedbackText != null) FontWeight.Bold else FontWeight.Normal,
                color = if (feedbackText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3x3 Burrows Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(9) { index ->
                    val critter = activeCritters[index]
                    val hasCritter = critter != null

                    val bounceScale by animateFloatAsState(
                        targetValue = if (hasCritter) 1f else 0.4f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "critterBounce"
                    )

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                            .clickable {
                                if (hasCritter && critter != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val points = if (critter.isGolden) 25 else 10
                                    val bonusMultiplier = if (comboStreak >= 4) 2 else 1
                                    val totalEarned = points * bonusMultiplier

                                    score += totalEarned
                                    crittersPopped++
                                    comboStreak++
                                    feedbackText = if (critter.isGolden) "⭐ Super Golden Pop! +$totalEarned pts" else "✨ Pop! +$totalEarned pts"

                                    onScoreEarned(totalEarned)
                                    activeCritters.remove(index)
                                } else {
                                    comboStreak = 0
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Burrow hole shadow
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        )

                        // Animated Critter
                        if (hasCritter && critter != null) {
                            Box(
                                modifier = Modifier
                                    .scale(bounceScale)
                                    .graphicsLayer { alpha = bounceScale },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = critter.emoji,
                                    fontSize = if (critter.isGolden) 40.sp else 36.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
