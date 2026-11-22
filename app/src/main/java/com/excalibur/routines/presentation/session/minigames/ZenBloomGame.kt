package com.excalibur.routines.presentation.session.minigames

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ZenRipple(
    val id: Long = Random.nextLong(),
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float = Random.nextFloat() * 15f + 25f
)

@Composable
fun ZenBloomGame(
    modifier: Modifier = Modifier
) {
    var completedCycles by remember { mutableIntStateOf(0) }
    var zenRipplesTapped by remember { mutableIntStateOf(0) }
    val floatingRipples = remember { mutableStateListOf<ZenRipple>() }
    val haptic = LocalHapticFeedback.current

    // Infinite breathing cycle: 16 seconds total (4s inhale, 4s hold, 4s exhale, 4s rest)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 16000
                0.5f at 0 using FastOutSlowInEasing
                1.0f at 4000 // Inhale complete
                1.0f at 8000 // Hold
                0.5f at 12000 // Exhale complete
                0.5f at 16000 // Rest
            }
        ),
        label = "breathScale"
    )

    // Determine current breath stage text
    var breathStage by remember { mutableStateOf("Inhale slowly...") }
    LaunchedEffect(Unit) {
        while (true) {
            breathStage = "Inhale slowly..."
            delay(4000L)
            breathStage = "Hold gently..."
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(4000L)
            breathStage = "Exhale smoothly..."
            delay(4000L)
            breathStage = "Rest & relax..."
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(4000L)
            completedCycles++
        }
    }

    // Spawn gentle ambient zen droplets to tap
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200L)
            if (floatingRipples.size < 4) {
                floatingRipples.add(
                    ZenRipple(
                        xFraction = Random.nextFloat().coerceIn(0.15f, 0.85f),
                        yFraction = Random.nextFloat().coerceIn(0.15f, 0.85f)
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Stats Header
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
                    text = "Breaths: $completedCycles 🪷",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Zen Taps: $zenRipplesTapped ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Breathing Visualizer Centerpiece
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary

            Canvas(modifier = Modifier.size(260.dp)) {
                val centerOffset = center
                val baseRadius = size.minDimension / 2f

                // Outer ambient halo
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.15f * breathScale),
                    radius = baseRadius * breathScale,
                    center = centerOffset
                )

                // Middle glowing ring
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f * breathScale),
                    radius = baseRadius * 0.75f * breathScale,
                    center = centerOffset
                )

                // Inner core ring
                drawCircle(
                    color = secondaryColor,
                    radius = baseRadius * 0.5f * breathScale,
                    center = centerOffset,
                    style = Stroke(width = 6f)
                )
            }

            // Central Breath Stage Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🪷",
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = breathStage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Interactive Floating Zen Ripples
            floatingRipples.forEach { ripple ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x3300E5FF),
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.TopStart)
                            .padding(
                                start = (ripple.xFraction * 240).dp,
                                top = (ripple.yFraction * 240).dp
                            )
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                floatingRipples.remove(ripple)
                                zenRipplesTapped++
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✨", fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        Text(
            text = "Follow the rhythm to recharge your mind • Tap floating sparks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
