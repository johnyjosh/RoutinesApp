package com.excalibur.routines.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.excalibur.routines.presentation.viewmodels.RoutineInstanceWithRoutine

@Composable
fun RoutineInstanceCard(
    routineInstanceWithRoutine: RoutineInstanceWithRoutine,
    onToggle: (RoutineInstanceWithRoutine) -> Unit,
    onEdit: (RoutineInstanceWithRoutine) -> Unit,
    onDelete: (RoutineInstanceWithRoutine) -> Unit,
    modifier: Modifier = Modifier
) {
    val instance = routineInstanceWithRoutine.instance
    val routine = routineInstanceWithRoutine.routine

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = routine?.name ?: "Unknown Routine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = instance.getScheduleString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    routine?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Duration: ${it.getTotalDurationString()} • ${it.timeIntervals.size} steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = instance.isEnabled,
                        onCheckedChange = { onToggle(routineInstanceWithRoutine) }
                    )
                    
                    IconButton(onClick = { onEdit(routineInstanceWithRoutine) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    
                    IconButton(onClick = { onDelete(routineInstanceWithRoutine) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}