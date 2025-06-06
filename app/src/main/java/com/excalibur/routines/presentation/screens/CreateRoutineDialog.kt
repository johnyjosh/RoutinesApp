package com.excalibur.routines.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.excalibur.routines.domain.models.Routine
import com.excalibur.routines.domain.models.TimeInterval
import java.util.UUID

@Composable
fun CreateRoutineDialog(
    editingRoutine: Routine? = null,
    onConfirm: (String, List<TimeInterval>) -> Unit,
    onDismiss: () -> Unit
) {
    var routineName by remember { mutableStateOf(editingRoutine?.name ?: "") }
    var timeIntervals by remember { mutableStateOf(editingRoutine?.timeIntervals ?: emptyList()) }
    
    val focusRequester = remember { FocusRequester() }
    val isEditing = editingRoutine != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Routine" else "Create Routine")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text("Routine Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time Intervals")
                    TextButton(
                        onClick = {
                            timeIntervals = timeIntervals + TimeInterval(
                                id = UUID.randomUUID().toString(),
                                name = "",
                                hours = 0,
                                minutes = 5
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add Step")
                    }
                }
                
                LazyColumn {
                    itemsIndexed(timeIntervals) { index, interval ->
                        TimeIntervalItem(
                            interval = interval,
                            onUpdate = { updatedInterval ->
                                timeIntervals = timeIntervals.toMutableList().apply {
                                    this[index] = updatedInterval
                                }
                            },
                            onDelete = {
                                timeIntervals = timeIntervals.filterIndexed { i, _ -> i != index }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(routineName, timeIntervals) },
                enabled = routineName.isNotBlank() && timeIntervals.isNotEmpty() && 
                         timeIntervals.all { it.name.isNotBlank() }
            ) {
                Text(if (isEditing) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun TimeIntervalItem(
    interval: TimeInterval,
    onUpdate: (TimeInterval) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = interval.name,
            onValueChange = { onUpdate(interval.copy(name = it)) },
            label = { Text("Step name") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        
        OutlinedTextField(
            value = interval.getHours().toString(),
            onValueChange = { hours ->
                val h = hours.toIntOrNull() ?: 0
                onUpdate(TimeInterval(interval.id, interval.name, h, interval.getMinutes()))
            },
            label = { Text("H") },
            modifier = Modifier.width(60.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        
        OutlinedTextField(
            value = interval.getMinutes().toString(),
            onValueChange = { minutes ->
                val m = minutes.toIntOrNull() ?: 0
                onUpdate(TimeInterval(interval.id, interval.name, interval.getHours(), m))
            },
            label = { Text("M") },
            modifier = Modifier.width(60.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}