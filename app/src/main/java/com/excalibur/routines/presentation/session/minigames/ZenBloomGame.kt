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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ZenSpark(
    val id: Long = Random.nextLong(),
    val xFraction: Float,
    val yFraction: Float
)

@Composable
fun ZenBloomGame(
    onScoreEarned: (points: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var completedCycles by remember { mutableIntStateOf(0) }
    var zenRipplesTapped by remember { mutableIntStateOf(0) }
    val floatingSparks = remember {
        mutableStateListOf(
            ZenSpark(xFraction = 0.2f, yFraction = 0.25f),
            ZenSpark(xFraction = 0.8f, yFraction = 0.35f),
            ZenSpark(xFraction = 0.3f, yFraction = 0.75f)
        )
    }
    val haptic = LocalHapticFeedback.current

    // Infinite breathing cycle: 16 seconds total (4s inhale, 4s hold, 4s exhale, 4s rest)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 16000
                0.55f at 0 using FastOutSlowInEasing
                1.0f at 4000 // Inhale complete
                1.0f at 8000 // Hold
                0.55f at 12000 // Exhale complete
                0.55f at 16000 // Rest
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
            onScoreEarned(15)
        }
    }

    // Spawn gentle floating sparks to tap
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800L)
            if (floatingSparks.size < 5) {
                floatingSparks.add(
                    ZenSpark(
                        xFraction = Random.nextFloat().coerceIn(0.12f, 0.88f),
                        yFraction = Random.nextFloat().coerceIn(0.12f, 0.88f)
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stats Header
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
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
                    text = "Zen Sparks: $zenRipplesTapped ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Full Interactive Ambient Playfield
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val density = LocalDensity.current
            val playfieldWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
            val playfieldHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
            val sparkSizePx = with(density) { 52.dp.toPx() }

            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary

            // Perfectly Centered Breathing Lotus Circle
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOffset = center
                    val baseRadius = size.minDimension / 2f

                    // Outer ambient glowing halo
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.15f * breathScale),
                        radius = baseRadius * breathScale,
                        center = centerOffset
                    )

                    // Middle pulse ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.32f * breathScale),
                        radius = baseRadius * 0.72f * breathScale,
                        center = centerOffset
                    )

                    // Inner core boundary ring
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.75f),
                        radius = baseRadius * 0.48f * breathScale,
                        center = centerOffset,
                        style = Stroke(width = 5f)
                    )
                }

                // Lotus emoji directly centered inside the circle
                Text(
                    text = "🪷",
                    fontSize = 54.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Interactive Floating Zen Sparks with Tap Ripples
            floatingSparks.forEach { spark ->
                val xPx = (spark.xFraction * (playfieldWidthPx - sparkSizePx)).toInt()
                val yPx = (spark.yFraction * (playfieldHeightPx - sparkSizePx)).toInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                        .clickable {
                            zenRipplesTapped++
                            floatingSparks.remove(spark)
                            onScoreEarned(10)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✨", fontSize = 24.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Breathing Guidance Text (Centered below the glowing circle)
        Text(
            text = breathStage,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Follow the rhythm to recharge your mind • Tap floating sparks ✨",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
