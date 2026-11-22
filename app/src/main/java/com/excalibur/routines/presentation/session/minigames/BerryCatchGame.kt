package com.excalibur.routines.presentation.session.minigames

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.random.Random

data class FallingTreat(
    val id: Long = Random.nextLong(),
    val emoji: String,
    val xFraction: Float,
    val yFraction: Float,
    val speed: Float // units per second (e.g. 0.45f -> takes ~2.2s to fall full screen)
)

@Composable
fun BerryCatchGame(
    companionIcon: String = "🦊",
    onScoreEarned: (points: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var score by remember { mutableIntStateOf(0) }
    var caughtCount by remember { mutableIntStateOf(0) }
    var basketX by remember { mutableFloatStateOf(0.5f) } // 0.0 to 1.0 fraction
    var treats by remember { mutableStateOf<List<FallingTreat>>(emptyList()) }
    val haptic = LocalHapticFeedback.current

    val treatEmojis = remember { listOf("🍓", "⭐", "🫐", "🍎", "🍒", "🍇") }

    // Smooth 60fps/120fps delta-time game loop independent of touch inputs
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        var spawnAccumulatorMs = 0L

        while (isActive) {
            withFrameMillis { now ->
                val dtMs = (now - lastTime).coerceIn(1L, 40L)
                lastTime = now
                val dtSec = dtMs / 1000f

                spawnAccumulatorMs += dtMs

                val updatedTreats = mutableListOf<FallingTreat>()

                // Spawn new treat every 800ms if fewer than 5 treats
                if (spawnAccumulatorMs >= 800L && treats.size < 5) {
                    spawnAccumulatorMs = 0L
                    updatedTreats.add(
                        FallingTreat(
                            emoji = treatEmojis.random(),
                            xFraction = Random.nextFloat().coerceIn(0.1f, 0.9f),
                            yFraction = 0f,
                            speed = 0.38f + Random.nextFloat() * 0.16f
                        )
                    )
                }

                // Advance treat positions and detect catches
                for (treat in treats) {
                    val nextY = treat.yFraction + (treat.speed * dtSec)

                    // Collision detection with companion basket (at y ~ 0.85)
                    if (nextY in 0.80f..0.91f && abs(treat.xFraction - basketX) < 0.16f) {
                        score += 10
                        caughtCount++
                        onScoreEarned(10)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (nextY <= 1.05f) {
                        updatedTreats.add(treat.copy(yFraction = nextY))
                    }
                }

                treats = updatedTreats
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
            text = "Tap or slide left/right to catch treats!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        // Game Playfield with 60fps Rendering
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        basketX = (offset.x / size.width).coerceIn(0.08f, 0.92f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        basketX = (change.position.x / size.width).coerceIn(0.08f, 0.92f)
                    }
                }
        ) {
            val density = LocalDensity.current
            val playfieldWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
            val playfieldHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
            val treatSizePx = with(density) { 36.dp.toPx() }
            val companionSizePx = with(density) { 56.dp.toPx() }

            // Render Falling Treats
            treats.forEach { treat ->
                val xPx = (treat.xFraction * (playfieldWidthPx - treatSizePx)).toInt()
                val yPx = (treat.yFraction * playfieldHeightPx).toInt()

                Text(
                    text = treat.emoji,
                    fontSize = 30.sp,
                    modifier = Modifier.offset { IntOffset(xPx, yPx) }
                )
            }

            // Render Companion at bottom (direct responsive tracking)
            val companionXPx = (basketX * (playfieldWidthPx - companionSizePx)).toInt()
            val companionYPx = (playfieldHeightPx * 0.84f).toInt()

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(56.dp)
                    .offset { IntOffset(companionXPx, companionYPx) },
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = companionIcon, fontSize = 32.sp)
                }
            }
        }
    }
}
