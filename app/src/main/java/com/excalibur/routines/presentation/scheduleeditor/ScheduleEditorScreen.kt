package com.excalibur.routines.presentation.scheduleeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditorScreen(
    viewModel: ScheduleEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val timePickerState = rememberTimePickerState(
        initialHour = uiState.startHour,
        initialMinute = uiState.startMinute,
        is24Hour = false
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        viewModel.setTime(timePickerState.hour, timePickerState.minute)
    }

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "Add Schedule" else "Edit Schedule") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(16.dp)
                                .size(24.dp)
                        )
                    } else {
                        IconButton(onClick = { viewModel.save(onSuccess = onNavigateBack) }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Routine Header Badge
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = uiState.avatar.emoji, fontSize = 24.sp)
                    Text(
                        text = uiState.routineName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Material 3 Time Picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Start Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 12.dp)
                    )
                    TimePicker(state = timePickerState)
                }
            }

            // Timeline Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
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
                        Text(
                            text = "Schedule Timeline Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val totalSecs = uiState.steps.sumOf { it.totalSeconds }
                        val totalMins = totalSecs / 60
                        if (totalMins > 0) {
                            Text(
                                text = "${totalMins}m total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.steps.isEmpty()) {
                        Text(
                            text = "No steps configured yet. Steps added in the Routine Editor will appear along this schedule timeline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val baseTime = LocalTime.of(uiState.startHour, uiState.startMinute)
                        var runningSeconds = 0

                        uiState.steps.forEachIndexed { index, step ->
                            val stepStartTime = baseTime.plusSeconds(runningSeconds.toLong())
                            runningSeconds += step.totalSeconds
                            val stepEndTime = baseTime.plusSeconds(runningSeconds.toLong())

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${stepStartTime.format(timeFormatter)} - ${stepEndTime.format(timeFormatter)} • ${step.formattedDuration}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (step.isForcefulAlarm) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "Forceful Alarm",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Alarm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Quick Chime",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Chime",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Final Routine Completion Event
                        val completionTime = baseTime.plusSeconds(runningSeconds.toLong())
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = "Completion",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Routine Complete at ${completionTime.format(timeFormatter)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Plays a celebration chime to mark all steps completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recurrence Days Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Repeat Days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val weekdays = setOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        )
                        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        val allDays = DayOfWeek.entries.toSet()

                        FilterChip(
                            selected = uiState.daysOfWeek == allDays,
                            onClick = { viewModel.setPresetDays(allDays) },
                            label = { Text("Every day") }
                        )
                        FilterChip(
                            selected = uiState.daysOfWeek == weekdays,
                            onClick = { viewModel.setPresetDays(weekdays) },
                            label = { Text("Weekdays") }
                        )
                        FilterChip(
                            selected = uiState.daysOfWeek == weekends,
                            onClick = { viewModel.setPresetDays(weekends) },
                            label = { Text("Weekends") }
                        )
                        FilterChip(
                            selected = uiState.daysOfWeek.isEmpty(),
                            onClick = { viewModel.setPresetDays(emptySet()) },
                            label = { Text("One-time") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Individual day toggle chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            val isSelected = uiState.daysOfWeek.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleDay(day) },
                                label = { Text(day.name.take(1)) },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            // Options: Sound and Vibration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Play Sound", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = uiState.soundEnabled,
                            onCheckedChange = viewModel::setSoundEnabled
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Vibrate", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = uiState.vibrateEnabled,
                            onCheckedChange = viewModel::setVibrateEnabled
                        )
                    }
                }
            }
        }
    }
}
