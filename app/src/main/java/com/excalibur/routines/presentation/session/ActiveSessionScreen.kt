package com.excalibur.routines.presentation.session

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    viewModel: ActiveSessionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    // Keep screen awake while routine is in progress
    val context = LocalContext.current
    DisposableEffect(uiState.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (uiState.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (uiState.isFinished) {
        RoutineCelebrationView(
            routineName = uiState.routine?.name ?: "Routine",
            routineIcon = uiState.routine?.icon ?: "🦊",
            totalSteps = uiState.steps.size,
            onDone = onNavigateBack
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${uiState.routine?.icon ?: "🦊"} ${uiState.routine?.name ?: "Routine"}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleKeepScreenOn) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Keep Screen On",
                            tint = if (uiState.keepScreenOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Countdown Progress Hero
            val animatedProgress by animateFloatAsState(
                targetValue = uiState.progressFraction,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "stepProgress"
            )

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 14.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Animated Progress Fill
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )

                // Inside the Circle: Avatar & Countdown
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.routine?.icon ?: "🦊",
                        fontSize = 42.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.formattedRemainingTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (uiState.isPaused) "PAUSED" else "Step ${uiState.currentStepIndex + 1} of ${uiState.steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Current Step Card
            val currentStep = uiState.currentStep
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CURRENT TASK",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentStep?.name ?: "Step",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Step Timeline List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.steps, key = { _, step -> step.id }) { index, step ->
                    val isPast = index < uiState.currentStepIndex
                    val isCurrent = index == uiState.currentStepIndex

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isPast -> MaterialTheme.colorScheme.primary
                                                isCurrent -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPast) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = step.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            Text(
                                text = step.formattedDuration,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Bottom Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = viewModel::skipStep,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Skip Step")
                }

                Button(
                    onClick = viewModel::togglePauseResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isPaused) "Resume" else "Pause")
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Routine?") },
            text = { Text("Are you sure you want to exit? Your progress in this active session will be stopped.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
}

@Composable
fun RoutineCelebrationView(
    routineName: String,
    routineIcon: String,
    totalSteps: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎉", fontSize = 60.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Routine Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You finished all $totalSteps steps of $routineIcon $routineName.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Done! Return to Routines", fontWeight = FontWeight.Bold)
        }
    }
}
