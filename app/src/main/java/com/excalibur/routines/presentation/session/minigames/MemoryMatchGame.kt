package com.excalibur.routines.presentation.session.minigames

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class MemoryCard(
    val id: Int,
    val emoji: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

@Composable
fun MemoryMatchGame(
    modifier: Modifier = Modifier
) {
    val emojis = remember { listOf("🦊", "🐻", "🐱", "🦁", "🐼", "🐨") }
    val cards = remember {
        mutableStateListOf<MemoryCard>().apply {
            val deck = (emojis + emojis).shuffled().mapIndexed { index, emoji ->
                MemoryCard(id = index, emoji = emoji)
            }
            addAll(deck)
        }
    }

    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var matchesFound by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    fun resetDeck() {
        val newDeck = (emojis + emojis).shuffled().mapIndexed { index, emoji ->
            MemoryCard(id = index, emoji = emoji)
        }
        cards.clear()
        cards.addAll(newDeck)
        selectedIndices = emptyList()
        matchesFound = 0
    }

    // Check for match when two cards are flipped
    LaunchedEffect(selectedIndices) {
        if (selectedIndices.size == 2) {
            val first = cards[selectedIndices[0]]
            val second = cards[selectedIndices[1]]

            if (first.emoji == second.emoji) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                cards[selectedIndices[0]] = first.copy(isMatched = true, isFaceUp = true)
                cards[selectedIndices[1]] = second.copy(isMatched = true, isFaceUp = true)
                matchesFound++
                score += 20
                selectedIndices = emptyList()

                if (matchesFound == 6) {
                    score += 50
                    delay(1200L)
                    resetDeck()
                }
            } else {
                delay(800L)
                cards[selectedIndices[0]] = first.copy(isFaceUp = false)
                cards[selectedIndices[1]] = second.copy(isFaceUp = false)
                selectedIndices = emptyList()
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
                    text = "Pairs: $matchesFound / 6",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Flip cards to find matching companion pairs!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 3 x 4 Grid of Memory Cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(cards) { index, card ->
                val rotation by animateFloatAsState(
                    targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
                    animationSpec = tween(durationMillis = 350),
                    label = "cardFlip"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.85f)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable(enabled = !card.isFaceUp && !card.isMatched && selectedIndices.size < 2) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cards[index] = card.copy(isFaceUp = true)
                            selectedIndices = selectedIndices + index
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (card.isMatched) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        } else if (card.isFaceUp) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation > 90f) {
                            Text(
                                text = card.emoji,
                                fontSize = 32.sp,
                                modifier = Modifier.graphicsLayer { rotationY = 180f }
                            )
                        } else {
                            Text(
                                text = "✨",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
