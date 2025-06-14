package com.excalibur.routines.presentation.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.RoutineInstance
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateRoutineInstanceScreen(
    availableRoutines: List<Routine>,
    editingInstance: RoutineInstance? = null,
    preSelectedRoutineId: String? = null,
    onSave: (String, String, LocalTime, Set<DayOfWeek>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    
    var selectedRoutineId by remember { 
        mutableStateOf(
            editingInstance?.routineId 
                ?: preSelectedRoutineId
                ?: availableRoutines.firstOrNull()?.id 
                ?: ""
        ) 
    }
    var scheduleName by remember {
        mutableStateOf(
            editingInstance?.name ?: ""
        )
    }
    var selectedTime by remember { 
        mutableStateOf(editingInstance?.startTime ?: LocalTime.of(7, 0)) 
    }
    var selectedDays by remember { 
        mutableStateOf(editingInstance?.daysOfWeek ?: emptySet()) 
    }
    
    var dropdownExpanded by remember { mutableStateOf(false) }
    val isEditing = editingInstance != null
    val selectedRoutine = availableRoutines.find { it.id == selectedRoutineId }
    val scrollState = rememberScrollState()
    
    // Set default name when routine changes (only if not editing and name is empty)
    if (!isEditing && scheduleName.isEmpty() && selectedRoutine != null) {
        scheduleName = selectedRoutine.name
    }
    
    val isValid = selectedRoutineId.isNotEmpty() && scheduleName.isNotBlank()
    
    // Handle Android back button
    BackHandler {
        onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (isEditing) "Edit Schedule" else "Schedule Routine") 
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(selectedRoutineId, scheduleName, selectedTime, selectedDays) },
                        enabled = isValid
                    ) {
                        Text(
                            text = if (isEditing) "UPDATE" else "SCHEDULE",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Routine Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Routine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (availableRoutines.isEmpty()) {
                        Text(
                            text = "No routines available. Create a routine first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedRoutine?.name ?: "Select Routine",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Routine") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            )
                            
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                availableRoutines.forEach { routine ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(
                                                    text = routine.name,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "Duration: ${routine.getTotalDurationString()} • ${routine.timeIntervals.size} steps",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedRoutineId = routine.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Show routine preview
                        selectedRoutine?.let { routine ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "Routine Preview",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    routine.timeIntervals.take(3).forEach { interval ->
                                        Text(
                                            text = "• ${interval.name} (${interval.getDisplayString()})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (routine.timeIntervals.size > 3) {
                                        Text(
                                            text = "... and ${routine.timeIntervals.size - 3} more steps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Schedule Name
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Schedule Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = scheduleName,
                        onValueChange = { scheduleName = it },
                        label = { Text("Name") },
                        placeholder = { Text("Enter schedule name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text(
                        text = "Give your schedule a descriptive name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Time Selection
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
                        Text(
                            text = "Start Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedTime = LocalTime.of(hour, minute)
                                    },
                                    selectedTime.hour,
                                    selectedTime.minute,
                                    false
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = selectedTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            // Days Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Schedule Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (selectedDays.isEmpty()) {
                            "One-time alarm (runs once at the scheduled time)"
                        } else {
                            "Weekly recurring alarm (runs every week on selected days)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Days of Week",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "Leave all days unselected for a one-time alarm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Start with Sunday, end with Saturday
                        val daysInOrder = listOf(
                            DayOfWeek.SUNDAY,
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY
                        )
                        
                        daysInOrder.forEach { day ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(day)) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                },
                                label = { 
                                    Text(
                                        text = day.getShortDisplayName(),
                                        style = MaterialTheme.typography.bodySmall
                                    ) 
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    if (selectedDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                                }
                            ) {
                                Text("Weekdays")
                            }
                            
                            TextButton(
                                onClick = {
                                    selectedDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                                }
                            ) {
                                Text("Weekends")
                            }
                            
                            TextButton(
                                onClick = {
                                    selectedDays = DayOfWeek.entries.toSet()
                                }
                            ) {
                                Text("Every Day")
                            }
                            
                            TextButton(
                                onClick = {
                                    selectedDays = emptySet()
                                }
                            ) {
                                Text("Clear All")
                            }
                        }
                    }
                }
            }
            
            // Extra bottom padding to ensure the last field can scroll above keyboard
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

private fun DayOfWeek.getShortDisplayName(): String {
    return when (this) {
        DayOfWeek.SUNDAY -> "S"
        DayOfWeek.MONDAY -> "M"
        DayOfWeek.TUESDAY -> "T"
        DayOfWeek.WEDNESDAY -> "W"
        DayOfWeek.THURSDAY -> "T"
        DayOfWeek.FRIDAY -> "F"
        DayOfWeek.SATURDAY -> "S"
    }
}