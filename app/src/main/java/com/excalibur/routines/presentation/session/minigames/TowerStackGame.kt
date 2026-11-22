package com.excalibur.routines.presentation.session.minigames

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

data class StackBlock(
    val floor: Int,
    val centerFraction: Float,
    val widthFraction: Float,
    val color: Color
)

@Composable
fun TowerStackGame(
    onScoreEarned: (points: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val blockColors = remember {
        listOf(
            Color(0xFFFF8A80),
            Color(0xFFFFB74D),
            Color(0xFFFFD54F),
            Color(0xFF81C784),
            Color(0xFF4DB6AC),
            Color(0xFF4FC3F7),
            Color(0xFF7986CB),
            Color(0xFFBA68C8),
            Color(0xFFFF80AB)
        )
    }

    var score by remember { mutableIntStateOf(0) }
    var currentFloor by remember { mutableIntStateOf(1) }
    var perfectCombo by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    // Current sliding block state
    var currentBlockWidth by remember { mutableFloatStateOf(0.55f) }
    var currentBlockCenter by remember { mutableFloatStateOf(0.5f) }
    var moveDirection by remember { mutableFloatStateOf(1f) } // 1 = right, -1 = left

    // Tower blocks list (bottom to top)
    val placedBlocks = remember {
        mutableStateListOf(
            StackBlock(
                floor = 0,
                centerFraction = 0.5f,
                widthFraction = 0.55f,
                color = blockColors[0]
            )
        )
    }

    fun restartGame() {
        placedBlocks.clear()
        placedBlocks.add(
            StackBlock(
                floor = 0,
                centerFraction = 0.5f,
                widthFraction = 0.55f,
                color = blockColors[0]
            )
        )
        currentFloor = 1
        currentBlockWidth = 0.55f
        currentBlockCenter = 0.1f
        moveDirection = 1f
        perfectCombo = 0
        isGameOver = false
        feedbackText = null
    }

    // Game loop for sliding block
    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect

        var lastTime = withFrameMillis { it }
        while (isActive && !isGameOver) {
            withFrameMillis { now ->
                val dtMs = (now - lastTime).coerceIn(1L, 40L)
                lastTime = now
                val dtSec = dtMs / 1000f

                val speed = (0.75f + currentFloor * 0.02f).coerceAtMost(1.35f)
                val newCenter = currentBlockCenter + moveDirection * speed * dtSec

                val halfWidth = currentBlockWidth / 2f
                if (newCenter + halfWidth >= 1.0f) {
                    currentBlockCenter = 1.0f - halfWidth
                    moveDirection = -1f
                } else if (newCenter - halfWidth <= 0.0f) {
                    currentBlockCenter = halfWidth
                    moveDirection = 1f
                } else {
                    currentBlockCenter = newCenter
                }
            }
        }
    }

    LaunchedEffect(feedbackText) {
        if (feedbackText != null) {
            delay(1000L)
            feedbackText = null
        }
    }

    // Tap to drop block
    fun dropBlock() {
        if (isGameOver) {
            restartGame()
            return
        }

        val topPlaced = placedBlocks.last()
        val diff = currentBlockCenter - topPlaced.centerFraction
        val absDiff = abs(diff)

        if (absDiff < 0.035f) {
            // Perfect drop!
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            perfectCombo++
            val pts = 20 * (if (perfectCombo >= 3) 2 else 1)
            score += pts
            onScoreEarned(pts)
            feedbackText = if (perfectCombo >= 3) "🔥 SUPER PERFECT! +$pts" else "✨ PERFECT! +$pts"

            placedBlocks.add(
                StackBlock(
                    floor = currentFloor,
                    centerFraction = topPlaced.centerFraction, // Snaps perfectly
                    widthFraction = currentBlockWidth,
                    color = blockColors[currentFloor % blockColors.size]
                )
            )
            currentFloor++
        } else if (absDiff < topPlaced.widthFraction) {
            // Cut overhang
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            perfectCombo = 0
            val newWidth = currentBlockWidth - absDiff

            if (newWidth < 0.08f) {
                // Too narrow to support next floor
                isGameOver = true
                feedbackText = "Stack fell! Tower reached Floor $currentFloor 🏆"
            } else {
                val newCenter = (currentBlockCenter + topPlaced.centerFraction) / 2f
                val pts = 10
                score += pts
                onScoreEarned(pts)
                feedbackText = "+$pts pts"

                placedBlocks.add(
                    StackBlock(
                        floor = currentFloor,
                        centerFraction = newCenter,
                        widthFraction = newWidth,
                        color = blockColors[currentFloor % blockColors.size]
                    )
                )
                currentBlockWidth = newWidth
                currentFloor++
            }
        } else {
            // Completely missed
            isGameOver = true
            feedbackText = "Missed! Tower reached Floor $currentFloor 🏆"
        }
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable { dropBlock() },
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
                        Text("🏗️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Floor $currentFloor",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (perfectCombo >= 2) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "✨ ${perfectCombo}x Perfect!",
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = feedbackText ?: (if (isGameOver) "Tap anywhere to play again!" else "Tap screen to drop & stack!"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (feedbackText != null) FontWeight.Bold else FontWeight.Normal,
                color = if (feedbackText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Playfield Box
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                val arenaWidth = this.maxWidth
                val arenaHeight = this.maxHeight
                val blockHeight = 28.dp

                // Visible window of blocks (keep top ~8 floors in view)
                val visibleCount = 8
                val startIndex = (placedBlocks.size - visibleCount).coerceAtLeast(0)
                val visibleBlocks = placedBlocks.subList(startIndex, placedBlocks.size)

                // Render placed blocks from bottom upwards
                visibleBlocks.forEachIndexed { i, block ->
                    val blockBottomOffset = (i * 30).dp + 20.dp
                    val blockWidth = arenaWidth * block.widthFraction
                    val blockLeft = arenaWidth * (block.centerFraction - block.widthFraction / 2f)

                    Box(
                        modifier = Modifier
                            .offset(x = blockLeft, y = arenaHeight - blockBottomOffset - blockHeight)
                            .width(blockWidth)
                            .height(blockHeight)
                            .clip(RoundedCornerShape(6.dp))
                            .background(block.color)
                    )
                }

                // Render current sliding block at current floor level
                if (!isGameOver) {
                    val slidingBottomOffset = (visibleBlocks.size * 30).dp + 20.dp
                    val slidingWidth = arenaWidth * currentBlockWidth
                    val slidingLeft = arenaWidth * (currentBlockCenter - currentBlockWidth / 2f)
                    val activeColor = blockColors[currentFloor % blockColors.size]

                    Box(
                        modifier = Modifier
                            .offset(x = slidingLeft, y = arenaHeight - slidingBottomOffset - blockHeight)
                            .width(slidingWidth)
                            .height(blockHeight)
                            .clip(RoundedCornerShape(6.dp))
                            .background(activeColor)
                    )
                }
            }
        }
    }
}
