package com.excalibur.routines.presentation.routinelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.excalibur.routines.domain.model.RoutineSchedule
import com.excalibur.routines.domain.model.RoutineWithDetails
import com.excalibur.routines.domain.model.UserStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    viewModel: RoutineListViewModel,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (routineId: String) -> Unit,
    onAddSchedule: (routineId: String) -> Unit,
    onEditSchedule: (routineId: String, scheduleId: String) -> Unit,
    onStartRoutine: (routineId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    var routineToDelete by remember { mutableStateOf<RoutineWithDetails?>(null) }
    var routineToPermanentlyDelete by remember { mutableStateOf<RoutineWithDetails?>(null) }
    var showStatsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Routines",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRoutine,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Routine", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.routines.isEmpty() && uiState.archivedRoutines.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UserGamificationCard(
                            userStats = uiState.userStats,
                            onClick = { showStatsDialog = true }
                        )
                        EmptyRoutinesState(
                            onCreateRoutine = onCreateRoutine,
                            onSelectTemplate = { name, icon, steps ->
                                viewModel.createStarterTemplate(name, icon, steps)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "gamification_header") {
                            UserGamificationCard(
                                userStats = uiState.userStats,
                                onClick = { showStatsDialog = true }
                            )
                        }

                        if (uiState.routines.isEmpty()) {
                            item {
                                EmptyRoutinesState(
                                    onCreateRoutine = onCreateRoutine,
                                    onSelectTemplate = { name, icon, steps ->
                                        viewModel.createStarterTemplate(name, icon, steps)
                                    }
                                )
                            }
                        } else {
                            items(uiState.routines, key = { it.routine.id }) { routineWithDetails ->
                                RoutineCard(
                                    routineWithDetails = routineWithDetails,
                                    onStart = { onStartRoutine(routineWithDetails.routine.id) },
                                    onEdit = { onEditRoutine(routineWithDetails.routine.id) },
                                    onDuplicate = {
                                        viewModel.duplicateRoutine(routineWithDetails)
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "✨ Duplicated as '${routineWithDetails.routine.name} (Copy)'",
                                                duration = SnackbarDuration.Short
                                            )
                                            delay(150)
                                            listState.animateScrollToItem(uiState.routines.size)
                                        }
                                    },
                                    onDelete = { routineToDelete = routineWithDetails },
                                    onAddSchedule = { onAddSchedule(routineWithDetails.routine.id) },
                                    onEditSchedule = { scheduleId ->
                                        onEditSchedule(routineWithDetails.routine.id, scheduleId)
                                    },
                                    onToggleSchedule = { scheduleId, enabled ->
                                        viewModel.toggleSchedule(scheduleId, enabled)
                                    },
                                    onDeleteSchedule = { scheduleId ->
                                        viewModel.deleteSchedule(scheduleId)
                                    }
                                )
                            }
                        }

                        if (uiState.archivedRoutines.isNotEmpty()) {
                            item {
                                TrashSection(
                                    archivedRoutines = uiState.archivedRoutines,
                                    onRestore = { item ->
                                        viewModel.restoreRoutine(item.routine.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "'${item.routine.name}' restored",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onPermanentlyDelete = { routineToPermanentlyDelete = it },
                                    onExpand = {
                                        coroutineScope.launch {
                                            delay(250)
                                            val trashIndex = if (uiState.routines.isEmpty()) 1 else uiState.routines.size
                                            listState.animateScrollToItem(trashIndex)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    routineToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Move to Trash?") },
            text = { Text("Move '${item.routine.icon} ${item.routine.name}' to Trash? You can restore it anytime within 30 days before it is permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = item
                        viewModel.deleteRoutine(toDelete.routine.id)
                        routineToDelete = null
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "'${toDelete.routine.name}' moved to Trash",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreRoutine(toDelete.routine.id)
                            }
                        }
                    }
                ) {
                    Text("Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    routineToPermanentlyDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { routineToPermanentlyDelete = null },
            title = { Text("Delete Forever?") },
            text = { Text("Permanently delete '${item.routine.icon} ${item.routine.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteRoutine(item.routine.id)
                        routineToPermanentlyDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToPermanentlyDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStatsDialog) {
        StatsBreakdownDialog(
            userStats = uiState.userStats,
            onDismiss = { showStatsDialog = false }
        )
    }
}

@Composable
fun RoutineCard(
    routineWithDetails: RoutineWithDetails,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: (String) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isCopying by remember { mutableStateOf(false) }
    val copyScale by animateFloatAsState(
        targetValue = if (isCopying) 1.35f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "copyScale"
    )

    LaunchedEffect(isCopying) {
        if (isCopying) {
            delay(700)
            isCopying = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Animal Avatar, Title, Duration Badge & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = routineWithDetails.routine.icon, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = routineWithDetails.routine.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = routineWithDetails.formattedTotalDuration,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${routineWithDetails.steps.size} steps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            isCopying = true
                            onDuplicate()
                        }
                    ) {
                        Icon(
                            imageVector = if (isCopying) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = if (isCopying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(copyScale)
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Quick Run Button & Expand Steps Row
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // "Start Routine" Button
                FilledTonalButton(
                    onClick = onStart,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Routine", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide steps" else "Show steps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    routineWithDetails.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${step.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (step.isForcefulAlarm) "🚨" else "🔔",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = step.formattedDuration,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Schedules Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Schedules (${routineWithDetails.schedules.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onAddSchedule) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Schedule", fontWeight = FontWeight.SemiBold)
                }
            }

            if (routineWithDetails.schedules.isEmpty()) {
                Text(
                    text = "No alarm schedules set yet. Tap 'Add Schedule' to set exact alarm times.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    routineWithDetails.schedules.forEach { schedule ->
                        ScheduleItemRow(
                            schedule = schedule,
                            onToggle = { enabled -> onToggleSchedule(schedule.id, enabled) },
                            onEdit = { onEditSchedule(schedule.id) },
                            onDelete = { onDeleteSchedule(schedule.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItemRow(
    schedule: RoutineSchedule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (schedule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = schedule.formattedTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = schedule.recurrenceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Visible Action Row with Edit & Delete & Switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Schedule",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Schedule",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

@Composable
fun EmptyRoutinesState(
    onCreateRoutine: () -> Unit,
    onSelectTemplate: (name: String, icon: String, steps: List<Pair<String, Int>>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🦊", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to Routines",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose a starter routine template below or build your own from scratch:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Starter Template Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StarterTemplateCard(
                icon = "🌅",
                title = "Morning Energizer",
                steps = "Stretch (10m) → Shower (15m) → Breakfast (20m)",
                onClick = {
                    onSelectTemplate(
                        "Morning Energizer",
                        "🦊",
                        listOf("Stretch & Yoga" to 10, "Shower & Dress" to 15, "Healthy Breakfast" to 20)
                    )
                }
            )

            StarterTemplateCard(
                icon = "🍅",
                title = "Pomodoro Focus Block",
                steps = "Deep Work (25m) → Rest Break (5m)",
                onClick = {
                    onSelectTemplate(
                        "Pomodoro Focus Block",
                        "🦉",
                        listOf("Deep Focus Session" to 25, "Stretch & Recharge Break" to 5)
                    )
                }
            )

            StarterTemplateCard(
                icon = "🌙",
                title = "Evening Wind Down",
                steps = "Read (15m) → Meditation (10m) → Sleep",
                onClick = {
                    onSelectTemplate(
                        "Evening Wind Down",
                        "🐻",
                        listOf("Screen-Free Reading" to 15, "Mindful Meditation" to 10)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onCreateRoutine,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Custom Routine", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StarterTemplateCard(
    icon: String,
    title: String,
    steps: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = steps, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun TrashSection(
    archivedRoutines: List<RoutineWithDetails>,
    onRestore: (RoutineWithDetails) -> Unit,
    onPermanentlyDelete: (RoutineWithDetails) -> Unit,
    onExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    fun toggleExpanded() {
        isExpanded = !isExpanded
        if (isExpanded) {
            onExpand?.invoke()
            coroutineScope.launch {
                delay(200)
                bringIntoViewRequester.bringIntoView()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { toggleExpanded() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Trash (${archivedRoutines.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-purges routines after 30 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(onClick = { toggleExpanded() }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    archivedRoutines.forEach { item ->
                        val routine = item.routine
                        val archivedMillis = routine.archivedAt ?: System.currentTimeMillis()
                        val daysSinceArchived = ((System.currentTimeMillis() - archivedMillis) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                        val daysRemaining = (30 - daysSinceArchived).coerceAtLeast(0)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = routine.icon, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            text = routine.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${item.steps.size} steps • ${daysRemaining}d left",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onRestore(item) }) {
                                        Text("Restore")
                                    }
                                    IconButton(onClick = { onPermanentlyDelete(item) }) {
                                        Icon(
                                            Icons.Default.DeleteForever,
                                            contentDescription = "Delete Forever",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserGamificationCard(
    userStats: UserStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Level ${userStats.level} • ${userStats.levelTitle}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userStats.currentStreak} Day Streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "TOTAL POINTS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${userStats.totalPoints} pts",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${userStats.pointsToNextLevel} pts to next level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { userStats.levelProgressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun StatsBreakdownDialog(
    userStats: UserStats,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lifetime Stats & Streaks", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⭐ Total Points", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${userStats.totalPoints} pts",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🔥 Current Streak", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${userStats.currentStreak} days",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⚡ Best Streak", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${userStats.bestStreak} days",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("✅ Routines Completed", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${userStats.routinesCompletedCount}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "How Points Work:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• +10 pts for each step you complete\n" +
                            "• +50 pts for finishing early & visiting Bonus Lounge\n" +
                            "• +10-20 pts for catching treats & mini-games\n" +
                            "• +100 pts for completing an entire routine\n\n" +
                            "Complete at least one routine every day to keep your streak burning!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it! 🚀")
            }
        }
    )
}

