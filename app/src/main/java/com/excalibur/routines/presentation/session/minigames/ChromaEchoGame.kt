package com.excalibur.routines.presentation.session.minigames

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ChromaPad(
    val id: Int,
    val name: String,
    val emoji: String,
    val baseColor: Color
)

@Composable
fun ChromaEchoGame(
    onScoreEarned: (points: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pads = remember {
        listOf(
            ChromaPad(0, "Ruby Fox", "🦊", Color(0xFFFF5252)),
            ChromaPad(1, "Golden Bear", "🐻", Color(0xFFFFD740)),
            ChromaPad(2, "Mint Panda", "🐼", Color(0xFF00E676)),
            ChromaPad(3, "Cosmic Cat", "🐱", Color(0xFF448AFF))
        )
    }

    val sequence = remember { mutableStateListOf(0, 1) }
    var activeLitPad by remember { mutableStateOf<Int?>(null) }
    var isDisplayingSequence by remember { mutableStateOf(false) }
    var playerStepIndex by remember { mutableIntStateOf(0) }
    var currentRound by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    // Trigger to play sequence
    var playSequenceTrigger by remember { mutableIntStateOf(1) }

    fun restartGame() {
        sequence.clear()
        sequence.addAll(listOf(Random.nextInt(4), Random.nextInt(4)))
        playerStepIndex = 0
        currentRound = 1
        activeLitPad = null
        feedbackText = null
        playSequenceTrigger++
    }

    // Playback sequence loop
    LaunchedEffect(playSequenceTrigger) {
        isDisplayingSequence = true
        playerStepIndex = 0
        activeLitPad = null
        delay(600L)

        for (padIndex in sequence) {
            activeLitPad = padIndex
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(500L)
            activeLitPad = null
            delay(250L)
        }

        isDisplayingSequence = false
    }

    LaunchedEffect(feedbackText) {
        if (feedbackText != null) {
            delay(1200L)
            feedbackText = null
        }
    }

    // Handle user tap on a pad
    fun onPadTapped(index: Int) {
        if (isDisplayingSequence) return

        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        activeLitPad = index

        if (index == sequence[playerStepIndex]) {
            // Correct step
            playerStepIndex++
            if (playerStepIndex >= sequence.size) {
                // Completed whole sequence!
                val roundPts = sequence.size * 10
                score += roundPts
                onScoreEarned(roundPts)
                feedbackText = "✨ Round $currentRound Clear! +$roundPts pts"
                currentRound++

                // Add next step to sequence
                sequence.add(Random.nextInt(4))
                playSequenceTrigger++
            }
        } else {
            // Incorrect step
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            feedbackText = "Oops! Round $currentRound reached 🏆"
            restartGame()
        }
    }

    // Clear active lit pad after quick tap release
    LaunchedEffect(activeLitPad) {
        if (activeLitPad != null && !isDisplayingSequence) {
            delay(250L)
            activeLitPad = null
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
            // Header stats
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
                        Text("🎨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Round $currentRound",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = feedbackText ?: (if (isDisplayingSequence) "👀 Watch the glowing pattern..." else "👉 Your turn! Repeat the pattern!"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (feedbackText != null) FontWeight.Bold else FontWeight.Normal,
                color = if (feedbackText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Glowing Pads
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Row 1: Pads 0 & 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PadButton(
                        pad = pads[0],
                        isLit = activeLitPad == 0,
                        onClick = { onPadTapped(0) },
                        modifier = Modifier.weight(1f)
                    )
                    PadButton(
                        pad = pads[1],
                        isLit = activeLitPad == 1,
                        onClick = { onPadTapped(1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Pads 2 & 3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PadButton(
                        pad = pads[2],
                        isLit = activeLitPad == 2,
                        onClick = { onPadTapped(2) },
                        modifier = Modifier.weight(1f)
                    )
                    PadButton(
                        pad = pads[3],
                        isLit = activeLitPad == 3,
                        onClick = { onPadTapped(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PadButton(
    pad: ChromaPad,
    isLit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isLit) 1.04f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "padScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isLit) pad.baseColor
                else pad.baseColor.copy(alpha = 0.35f)
            )
            .clickable { onClick() }
            .graphicsLayer {
                shadowElevation = if (isLit) 16.dp.toPx() else 2.dp.toPx()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = pad.emoji,
                fontSize = if (isLit) 44.sp else 36.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pad.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLit) Color.Black else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
