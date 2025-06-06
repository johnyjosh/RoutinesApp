package com.excalibur.routines.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.TimeInterval
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    editingRoutine: Routine? = null,
    onSave: (String, List<TimeInterval>) -> Unit,
    onCancel: () -> Unit
) {
    var routineName by remember { mutableStateOf(editingRoutine?.name ?: "") }
    var timeIntervals by remember { mutableStateOf(editingRoutine?.timeIntervals?.toList() ?: emptyList()) }
    var focusNewStep by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val isEditing = editingRoutine != null
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    val isValid = routineName.isNotBlank() && timeIntervals.isNotEmpty() && 
                 timeIntervals.all { it.name.isNotBlank() && (it.getHours() > 0 || it.getMinutes() > 0) }
    
    // Handle Android back button
    BackHandler {
        onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (isEditing) "Edit Routine" else "Create Routine") 
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(routineName, timeIntervals) },
                        enabled = isValid
                    ) {
                        Text(
                            text = if (isEditing) "UPDATE" else "CREATE",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    timeIntervals = timeIntervals + TimeInterval(
                        id = UUID.randomUUID().toString(),
                        name = "",
                        hours = 0,
                        minutes = 5
                    )
                    focusNewStep = true
                    // Scroll to the bottom to show the new item
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Step")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Routine Name Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Routine Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = { Text("Routine Name") },
                        placeholder = { Text("e.g., Morning Workout, Study Session") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Time Intervals Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Routine Steps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (timeIntervals.isNotEmpty()) {
                                Text(
                                    text = "${timeIntervals.size} steps • Total: ${getTotalDurationString(timeIntervals)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (timeIntervals.isEmpty()) {
                        Text(
                            text = "Add steps to your routine using the + button",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timeIntervals.forEachIndexed { index, interval ->
                                TimeIntervalCard(
                                    stepNumber = index + 1,
                                    interval = interval,
                                    shouldFocus = focusNewStep && index == timeIntervals.size - 1,
                                    onUpdate = { updatedInterval ->
                                        timeIntervals = timeIntervals.toMutableList().apply {
                                            this[index] = updatedInterval
                                        }
                                        if (focusNewStep && index == timeIntervals.size - 1) {
                                            focusNewStep = false
                                        }
                                    },
                                    onDelete = {
                                        timeIntervals = timeIntervals.filterIndexed { i, _ -> i != index }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Extra bottom padding to ensure the last field can scroll above keyboard
            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun TimeIntervalCard(
    stepNumber: Int,
    interval: TimeInterval,
    onUpdate: (TimeInterval) -> Unit,
    onDelete: () -> Unit,
    shouldFocus: Boolean = false
) {
    val stepFocusRequester = remember { FocusRequester() }
    
    // Local state for text fields to handle empty states properly
    var hoursText by remember(interval.id, interval.getHours()) { 
        mutableStateOf(if (interval.getHours() == 0) "" else interval.getHours().toString()) 
    }
    var minutesText by remember(interval.id, interval.getMinutes()) { 
        mutableStateOf(if (interval.getMinutes() == 0) "" else interval.getMinutes().toString()) 
    }
    
    // Focus states
    var hoursFocused by remember { mutableStateOf(false) }
    var minutesFocused by remember { mutableStateOf(false) }
    
    // Validation states
    val hoursValue = hoursText.toIntOrNull() ?: 0
    val minutesValue = minutesText.toIntOrNull() ?: 0
    val isInvalidDuration = hoursValue == 0 && minutesValue == 0
    val showError = isInvalidDuration && !hoursFocused && !minutesFocused
    val hoursHasError = showError
    val minutesHasError = showError
    
    // Update the interval when values change (if valid)
    LaunchedEffect(hoursValue, minutesValue) {
        if (!isInvalidDuration) {
            onUpdate(TimeInterval(interval.id, interval.name, hoursValue, minutesValue))
        }
    }
    
    Card {
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
                    text = "Step $stepNumber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Step")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = interval.name,
                onValueChange = { onUpdate(interval.copy(name = it)) },
                label = { Text("Step Description") },
                placeholder = { Text("e.g., Warm-up stretches, Review notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(stepFocusRequester),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input ->
                        // Only allow digits
                        val filtered = input.filter { it.isDigit() }
                        if (filtered.length <= 2) { // Max 2 digits
                            val newValue = filtered.toIntOrNull()?.coerceIn(0, 12) ?: 0
                            hoursText = if (filtered.isEmpty()) "" else newValue.toString()
                        }
                    },
                    label = { Text("Hours") },
                    placeholder = { Text("0") },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            hoursFocused = focusState.isFocused
                            if (!focusState.isFocused && hoursText.isEmpty()) {
                                hoursText = "0"
                            }
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = hoursHasError
                )
                
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { input ->
                        // Only allow digits
                        val filtered = input.filter { it.isDigit() }
                        if (filtered.length <= 2) { // Max 2 digits
                            val newValue = filtered.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            minutesText = if (filtered.isEmpty()) "" else newValue.toString()
                        }
                    },
                    label = { Text("Minutes") },
                    placeholder = { Text("0") },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            minutesFocused = focusState.isFocused
                            if (!focusState.isFocused && minutesText.isEmpty()) {
                                minutesText = "0"
                            }
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = minutesHasError
                )
            }
            
            if (isInvalidDuration && (!hoursFocused && !minutesFocused)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Duration must be at least 1 minute",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (hoursValue > 0 || minutesValue > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Duration: ${TimeInterval("", "", hoursValue, minutesValue).getDisplayString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            stepFocusRequester.requestFocus()
        }
    }
}

private fun getTotalDurationString(timeIntervals: List<TimeInterval>): String {
    val totalMinutes = timeIntervals.sumOf { it.getDurationInMinutes() }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}