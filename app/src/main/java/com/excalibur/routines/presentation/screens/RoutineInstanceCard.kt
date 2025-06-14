package com.excalibur.routines.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import java.time.format.DateTimeFormatter

@Composable
fun RoutineInstanceCard(
    routineInstanceWithRoutine: RoutineInstanceWithRoutine,
    isExpanded: Boolean,
    onToggle: (RoutineInstanceWithRoutine) -> Unit,
    onEdit: (RoutineInstanceWithRoutine) -> Unit,
    onDelete: (RoutineInstanceWithRoutine) -> Unit,
    onExpandToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val instance = routineInstanceWithRoutine.instance
    val routine = routineInstanceWithRoutine.routine

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Main header - clickable for expansion
            Column(
                modifier = Modifier
                    .clickable { onExpandToggle(instance.id) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Schedule name (new field we added)
                        Text(
                            text = instance.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Routine name  
                        Text(
                            text = "Routine: ${routine?.name ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        
                        // Expansion arrow
                        IconButton(onClick = { onExpandToggle(instance.id) }) {
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp 
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand"
                            )
                        }
                    }
                }
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider()
                    
                    routine?.let { r ->
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Schedule Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Show each interval with timing
                            var currentTime = instance.startTime
                            r.timeIntervals.forEachIndexed { index, interval ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${interval.name}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Duration: ${interval.getDisplayString()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Text(
                                        text = "🔔 ${currentTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                if (index < r.timeIntervals.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    currentTime = currentTime.plusMinutes(interval.getDurationInMinutes())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}