package com.excalibur.routines.presentation.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.RoutineInstance
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateRoutineInstanceDialog(
    availableRoutines: List<Routine>,
    editingInstance: RoutineInstance? = null,
    onConfirm: (String, LocalTime, Set<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    var selectedRoutineId by remember { 
        mutableStateOf(
            editingInstance?.routineId 
                ?: availableRoutines.firstOrNull()?.id 
                ?: ""
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Routine Instance" else "Schedule Routine")
        },
        text = {
            Column {
                // Routine Selection
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
                                        Text(routine.name)
                                        Text(
                                            "Duration: ${routine.getTotalDurationString()}",
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Time Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start Time")
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
                        Text(selectedTime.format(DateTimeFormatter.ofPattern("h:mm a")))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Days Selection
                Text("Days of Week")
                Text(
                    text = if (selectedDays.isEmpty()) "One-time alarm" else "Weekly recurring alarm",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(day.getDisplayName()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedRoutineId, selectedTime, selectedDays) },
                enabled = selectedRoutineId.isNotEmpty()
            ) {
                Text(if (isEditing) "Update" else "Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun DayOfWeek.getDisplayName(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }
}