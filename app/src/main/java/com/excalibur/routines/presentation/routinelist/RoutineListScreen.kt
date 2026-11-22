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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.excalibur.routines.domain.model.CompanionAvatar
import com.excalibur.routines.domain.model.Routine
import com.excalibur.routines.domain.model.RoutineSchedule
import com.excalibur.routines.domain.model.RoutineStep
import com.excalibur.routines.domain.model.RoutineWithDetails
import com.excalibur.routines.domain.model.UserStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

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
    var editingRoutineId by remember { mutableStateOf<String?>(null) }
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
                .consumeWindowInsets(innerPadding)
                .imePadding()
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "gamification_header") {
                            UserGamificationCard(
                                userStats = uiState.userStats,
                                onClick = { showStatsDialog = true }
                            )
                        }

                        // Type & Enter Quick Routine Creator
                        item(key = "quick_create_routine") {
                            var newRoutineName by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = newRoutineName,
                                onValueChange = { newRoutineName = it },
                                placeholder = {
                                    Text(
                                        text = "Create a routine (e.g. Evening Wind Down)...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (newRoutineName.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val name = newRoutineName.trim()
                                                if (name.isNotEmpty()) {
                                                    viewModel.createQuickRoutine(name) { newId ->
                                                        editingRoutineId = newId
                                                    }
                                                    newRoutineName = ""
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Create Routine",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done,
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val name = newRoutineName.trim()
                                        if (name.isNotEmpty()) {
                                            viewModel.createQuickRoutine(name) { newId ->
                                                editingRoutineId = newId
                                            }
                                            newRoutineName = ""
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
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
                                val isEditing = editingRoutineId == routineWithDetails.routine.id
                                RoutineCard(
                                    routineWithDetails = routineWithDetails,
                                    userStats = uiState.userStats,
                                    isEditing = isEditing,
                                    onToggleEdit = {
                                        editingRoutineId = if (isEditing) null else routineWithDetails.routine.id
                                    },
                                    onUpdateRoutine = { routine, steps ->
                                        viewModel.updateRoutine(routine, steps)
                                    },
                                    onStart = { onStartRoutine(routineWithDetails.routine.id) },
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
                                    onAddScheduleInline = { hour, minute ->
                                        viewModel.addQuickSchedule(routineWithDetails.routine.id, hour, minute)
                                    },
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

fun parseTimeString(input: String): Pair<Int, Int>? {
    val clean = input.trim().lowercase()
    if (clean.isEmpty()) return null
    return try {
        val isPm = clean.endsWith("pm") || clean.endsWith("p")
        val isAm = clean.endsWith("am") || clean.endsWith("a")
        val digitsPart = clean.replace("[a-z\\s]".toRegex(), "")
        val parts = digitsPart.split(":")
        val rawHour: Int
        val rawMinute: Int
        if (parts.size >= 2) {
            rawHour = parts[0].toIntOrNull() ?: return null
            rawMinute = parts[1].toIntOrNull() ?: return null
        } else if (parts.size == 1 && digitsPart.length in 1..4) {
            if (digitsPart.length <= 2) {
                rawHour = digitsPart.toIntOrNull() ?: return null
                rawMinute = 0
            } else {
                val hourLen = if (digitsPart.length == 3) 1 else 2
                rawHour = digitsPart.substring(0, hourLen).toIntOrNull() ?: return null
                rawMinute = digitsPart.substring(hourLen).toIntOrNull() ?: return null
            }
        } else {
            return null
        }
        val hour = when {
            isPm && rawHour < 12 -> rawHour + 12
            isAm && rawHour == 12 -> 0
            else -> rawHour
        }
        if (hour in 0..23 && rawMinute in 0..59) {
            Pair(hour, rawMinute)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun RoutineCard(
    routineWithDetails: RoutineWithDetails,
    userStats: UserStats,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onUpdateRoutine: (Routine, List<RoutineStep>) -> Unit,
    onStart: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAddSchedule: () -> Unit,
    onAddScheduleInline: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onEditSchedule: (String) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onDeleteSchedule: (String) -> Unit
) {
    // Dynamic Estimated Finish Time
    val estimatedFinishTime = remember(routineWithDetails.totalDurationSeconds) {
        val finishTime = LocalTime.now().plusSeconds(routineWithDetails.totalDurationSeconds.toLong())
        finishTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    // Editable state when inline editing is active
    var editingName by remember(routineWithDetails.routine.id, isEditing) { mutableStateOf(routineWithDetails.routine.name) }
    var editingIcon by remember(routineWithDetails.routine.id, isEditing) { mutableStateOf(routineWithDetails.routine.icon) }
    var editingSteps by remember(routineWithDetails.routine.id, isEditing, routineWithDetails.steps) { mutableStateOf(routineWithDetails.steps) }
    var expandedStepId by remember { mutableStateOf<String?>(null) }
    var draggedStepIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var newStepInput by remember { mutableStateOf("") }
    val itemHeightPx = with(LocalDensity.current) { 72.dp.toPx() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Animal Avatar, Title, Duration Badge, Options Menu & Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleEdit() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isEditing) editingIcon else routineWithDetails.routine.icon, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) editingName else routineWithDetails.routine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            text = "• ${routineWithDetails.steps.size} steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "• 🏁 $estimatedFinishTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3-dot overflow options menu (Add Schedule, Duplicate, Delete)
                var showOptionsMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Schedule") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null) },
                            onClick = {
                                showOptionsMenu = false
                                onAddSchedule()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Routine") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = {
                                showOptionsMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Routine", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showOptionsMenu = false
                                onDelete()
                            }
                        )
                    }
                }

                // Expand / Collapse Chevron
                IconButton(onClick = onToggleEdit) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isEditing) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Run Action
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onStart,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Routine", fontWeight = FontWeight.Bold)
            }

            // Inline Accordion Editor with 3 Distinct Visual Container Sections
            AnimatedVisibility(visible = isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 1: Routine Details & Companion
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ROUTINE DETAILS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = editingName,
                                onValueChange = { newName ->
                                    editingName = newName
                                    onUpdateRoutine(
                                        routineWithDetails.routine.copy(name = newName, icon = editingIcon),
                                        editingSteps
                                    )
                                },
                                label = { Text("Routine Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Companion Avatar",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Level ${userStats.level}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserStats.ALL_COMPANIONS.forEach { companion ->
                                        val isUnlocked = userStats.isAvatarUnlocked(companion)
                                        val isSelected = editingIcon == companion.emoji

                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                        isUnlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                    }
                                                )
                                                .clickable(enabled = isUnlocked) {
                                                    editingIcon = companion.emoji
                                                    onUpdateRoutine(
                                                        routineWithDetails.routine.copy(name = editingName, icon = companion.emoji),
                                                        editingSteps
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = companion.emoji,
                                                fontSize = if (isSelected) 24.sp else 18.sp,
                                                modifier = Modifier.graphicsLayer {
                                                    alpha = if (isUnlocked) 1f else 0.3f
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: Steps & Quick Add Step (Type & Enter)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "STEPS (${editingSteps.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Drag ≡ to reorder",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                editingSteps.forEachIndexed { index, step ->
                                    val isExpanded = expandedStepId == step.id
                                    val isBeingDragged = draggedStepIndex == index

                                    InlineStepAccordionItem(
                                        step = step,
                                        index = index,
                                        isExpanded = isExpanded,
                                        isBeingDragged = isBeingDragged,
                                        dragOffsetY = dragOffsetY,
                                        onToggleExpand = {
                                            expandedStepId = if (isExpanded) null else step.id
                                        },
                                        onUpdateStep = { name, mins ->
                                            val updated = editingSteps.toMutableList()
                                            updated[index] = step.copy(
                                                name = name,
                                                durationMinutes = mins,
                                                isForcefulAlarm = true
                                            )
                                            editingSteps = updated
                                            onUpdateRoutine(
                                                routineWithDetails.routine.copy(name = editingName, icon = editingIcon),
                                                updated
                                            )
                                        },
                                        onDragStart = {
                                            draggedStepIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { deltaY ->
                                            dragOffsetY += deltaY
                                            val current = draggedStepIndex ?: return@InlineStepAccordionItem
                                            if (dragOffsetY > itemHeightPx * 0.7f && current < editingSteps.size - 1) {
                                                val updated = editingSteps.toMutableList()
                                                val item = updated.removeAt(current)
                                                updated.add(current + 1, item)
                                                editingSteps = updated
                                                draggedStepIndex = current + 1
                                                dragOffsetY -= itemHeightPx
                                                onUpdateRoutine(
                                                    routineWithDetails.routine.copy(name = editingName, icon = editingIcon),
                                                    updated
                                                )
                                            } else if (dragOffsetY < -itemHeightPx * 0.7f && current > 0) {
                                                val updated = editingSteps.toMutableList()
                                                val item = updated.removeAt(current)
                                                updated.add(current - 1, item)
                                                editingSteps = updated
                                                draggedStepIndex = current - 1
                                                dragOffsetY += itemHeightPx
                                                onUpdateRoutine(
                                                    routineWithDetails.routine.copy(name = editingName, icon = editingIcon),
                                                    updated
                                                )
                                            }
                                        },
                                        onDragEnd = {
                                            draggedStepIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDelete = {
                                            val updated = editingSteps.toMutableList()
                                            updated.removeAt(index)
                                            editingSteps = updated
                                            onUpdateRoutine(
                                                routineWithDetails.routine.copy(name = editingName, icon = editingIcon),
                                                updated
                                            )
                                        }
                                    )
                                }
                            }

                            // Quick Add Step Placeholder
                            OutlinedTextField(
                                value = newStepInput,
                                onValueChange = { newStepInput = it },
                                placeholder = {
                                    Text(
                                        text = "Add a step (e.g. Drink water, Stretch)...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (newStepInput.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val stepName = newStepInput.trim()
                                                if (stepName.isNotEmpty()) {
                                                    val newStep = RoutineStep(
                                                        id = UUID.randomUUID().toString(),
                                                        routineId = routineWithDetails.routine.id,
                                                        name = stepName,
                                                        orderIndex = editingSteps.size,
                                                        durationMinutes = 5,
                                                        durationSeconds = 0,
                                                        isForcefulAlarm = true
                                                    )
                                                    val updated = editingSteps + newStep
                                                    editingSteps = updated
                                                    newStepInput = ""
                                                    onUpdateRoutine(
                                                        routineWithDetails.routine.copy(name = editingName, icon = editingIcon),
                                                        updated
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Save Step",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done,
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val stepName = newStepInput.trim()
                                        if (stepName.isNotEmpty()) {
                                            val newStep = RoutineStep(
                                                id = UUID.randomUUID().toString(),
                                                routineId = routineWithDetails.routine.id,
                                                name = stepName,
                                                orderIndex = editingSteps.size,
                                                durationMinutes = 5,
                                                durationSeconds = 0,
                                                isForcefulAlarm = true
                                            )
                                            val updated = editingSteps + newStep
                                            editingSteps = updated
                                            newStepInput = ""
                                            onUpdateRoutine(
                                                routineWithDetails.routine.copy(name = editingName, icon = editingIcon),
                                                updated
                                            )
                                        }
                                    }
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // SECTION 3: Alarm Schedules & Type & Enter Quick Creator
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ALARM SCHEDULES (${routineWithDetails.schedules.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (routineWithDetails.schedules.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                            // Quick Time Presets
                            Text(
                                text = "Quick Add Presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Pair("7:00 AM", Pair(7, 0)),
                                    Pair("7:30 AM", Pair(7, 30)),
                                    Pair("8:00 AM", Pair(8, 0)),
                                    Pair("8:30 AM", Pair(8, 30)),
                                    Pair("9:00 PM", Pair(21, 0)),
                                    Pair("10:00 PM", Pair(22, 0))
                                ).forEach { (label, time) ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            onAddScheduleInline(time.first, time.second)
                                        },
                                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }

                            // Inline Custom Time Input (Type & Enter)
                            var scheduleInputText by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = scheduleInputText,
                                onValueChange = { scheduleInputText = it },
                                placeholder = {
                                    Text(
                                        text = "Add custom alarm (e.g. 6:45 AM, 21:30)...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (scheduleInputText.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                parseTimeString(scheduleInputText.trim())?.let { (h, m) ->
                                                    onAddScheduleInline(h, m)
                                                    scheduleInputText = ""
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Save Schedule",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        parseTimeString(scheduleInputText.trim())?.let { (h, m) ->
                                            onAddScheduleInline(h, m)
                                            scheduleInputText = ""
                                        }
                                    }
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Compact Schedules Summary (shown when collapsed only if schedules exist)
            if (!isEditing && routineWithDetails.schedules.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Schedules (${routineWithDetails.schedules.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onAddSchedule,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
fun InlineStepAccordionItem(
    step: RoutineStep,
    index: Int,
    isExpanded: Boolean,
    isBeingDragged: Boolean,
    dragOffsetY: Float,
    onToggleExpand: () -> Unit,
    onUpdateStep: (name: String, minutes: Int) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isBeingDragged) 10f else 1f)
            .graphicsLayer {
                if (isBeingDragged) {
                    translationY = dragOffsetY
                    scaleX = 1.03f
                    scaleY = 1.03f
                    shadowElevation = 16.dp.toPx()
                }
            }
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            // Summary row: Grab handle, step name, duration badge, expand chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Continuous floating drag handle
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(28.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDragStart()
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onDrag(dragAmount.y)
                                    },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "${index + 1}. ${step.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = step.formattedDuration,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Expanded details: Step name field, duration steppers & presets, delete button
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = step.name,
                        onValueChange = { newName ->
                            onUpdateStep(newName, step.durationMinutes)
                        },
                        label = { Text("Step Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Duration Stepper & Quick Presets
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val newMins = (step.durationMinutes - 1).coerceAtLeast(1)
                                    onUpdateStep(step.name, newMins)
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease duration")
                            }

                            Text(
                                text = "${step.durationMinutes} min",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    val newMins = (step.durationMinutes + 1).coerceAtMost(180)
                                    onUpdateStep(step.name, newMins)
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase duration")
                            }
                        }

                        // Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(1, 5, 10, 15, 20, 30).forEach { presetMins ->
                                FilterChip(
                                    selected = step.durationMinutes == presetMins,
                                    onClick = {
                                        onUpdateStep(step.name, presetMins)
                                    },
                                    label = { Text("${presetMins}m", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Delete step button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove Step")
                        }
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Schedule",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Schedule",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
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
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userStats.currentStreak} Day Streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧊", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userStats.streakFreezesAvailable} Freeze Available",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        style = MaterialTheme.typography.headlineSmall,
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

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { userStats.levelProgressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
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
                            Text("🧊 Streak Freezes", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${userStats.streakFreezesAvailable} available",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
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
                    text = "How Points & Streaks Work:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• +10 pts for each step you complete\n" +
                            "• +50 pts for finishing early & visiting Bonus Lounge\n" +
                            "• +10-20 pts for catching treats & mini-games\n" +
                            "• +100 pts for completing an entire routine\n\n" +
                            "🧊 Streak Freezes protect your streak if you miss a day! You earn 1 every 7 completions (up to 2).\n\n" +
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

