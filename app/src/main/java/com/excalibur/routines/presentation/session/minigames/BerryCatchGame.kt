package com.excalibur.routines.presentation.session.minigames

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

data class FallingTreat(
    val id: Long = Random.nextLong(),
    val emoji: String,
    val xFraction: Float,
    var yFraction: Float = 0f,
    val speed: Float = 0.015f
)

@Composable
fun BerryCatchGame(
    companionIcon: String = "🦊",
    modifier: Modifier = Modifier
) {
    var score by remember { mutableIntStateOf(0) }
    var caughtCount by remember { mutableIntStateOf(0) }
    var basketX by remember { mutableStateOf(0.5f) } // 0.0 to 1.0 fraction
    val treats = remember { mutableStateListOf<FallingTreat>() }
    val haptic = LocalHapticFeedback.current

    val treatEmojis = remember { listOf("🍓", "⭐", "🫐", "🍎", "🍒", "🍇") }

    // Game loop for falling treats and spawning
    LaunchedEffect(Unit) {
        var tickCount = 0
        while (true) {
            delay(32L) // ~30 fps
            tickCount++

            // Spawn treat every ~30 ticks (~1 second)
            if (tickCount % 28 == 0 && treats.size < 6) {
                treats.add(
                    FallingTreat(
                        emoji = treatEmojis.random(),
                        xFraction = Random.nextFloat().coerceIn(0.1f, 0.9f),
                        yFraction = 0f,
                        speed = (0.014f + Random.nextFloat() * 0.008f)
                    )
                )
            }

            // Update positions and handle collisions
            val iterator = treats.iterator()
            while (iterator.hasNext()) {
                val treat = iterator.next()
                treat.yFraction += treat.speed

                // Catch detection (companion is at y ~ 0.85)
                if (treat.yFraction in 0.80f..0.92f && abs(treat.xFraction - basketX) < 0.16f) {
                    iterator.remove()
                    score += 10
                    caughtCount++
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else if (treat.yFraction > 1.0f) {
                    iterator.remove()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score Header
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Caught: $caughtCount 🍓",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Tap or drag left/right to catch treats!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        // Game Playfield
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val tapFraction = (offset.x / size.width).coerceIn(0.08f, 0.92f)
                        basketX = tapFraction
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val dragFraction = (change.position.x / size.width).coerceIn(0.08f, 0.92f)
                        basketX = dragFraction
                    }
                }
        ) {
            val playfieldWidth = this.maxWidth
            val playfieldHeight = this.maxHeight

            // Render Falling Treats
            treats.forEach { treat ->
                val xOffset = playfieldWidth * treat.xFraction - 16.dp
                val yOffset = playfieldHeight * treat.yFraction

                Text(
                    text = treat.emoji,
                    fontSize = 28.sp,
                    modifier = Modifier.offset {
                        IntOffset(xOffset.roundToPx(), yOffset.roundToPx())
                    }
                )
            }

            // Render Companion at bottom
            val animatedBasketX by animateFloatAsState(targetValue = basketX, label = "basketX")
            val companionXOffset = playfieldWidth * animatedBasketX - 28.dp
            val companionYOffset = playfieldHeight * 0.84f

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(56.dp)
                    .offset {
                        IntOffset(companionXOffset.roundToPx(), companionYOffset.roundToPx())
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = companionIcon, fontSize = 32.sp)
                }
            }
        }
    }
}
