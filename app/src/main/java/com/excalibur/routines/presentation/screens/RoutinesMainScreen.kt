package com.excalibur.routines.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.excalibur.routines.presentation.viewmodels.RoutineInstanceViewModel
import com.excalibur.routines.presentation.viewmodels.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesMainScreen(
    routineViewModel: RoutineViewModel,
    routineInstanceViewModel: RoutineInstanceViewModel,
    modifier: Modifier = Modifier
) {
    val routines by routineViewModel.routines.collectAsState()
    val routineInstances by routineInstanceViewModel.routineInstances.collectAsState()
    val availableRoutines by routineInstanceViewModel.availableRoutines.collectAsState()
    val isLoadingRoutines by routineViewModel.isLoading.collectAsState()
    val isLoadingInstances by routineInstanceViewModel.isLoading.collectAsState()
    val routineError by routineViewModel.errorMessage.collectAsState()
    val instanceError by routineInstanceViewModel.errorMessage.collectAsState()
    
    val showCreateRoutineScreen by routineViewModel.showCreateScreen.collectAsState()
    val editingRoutine by routineViewModel.editingRoutine.collectAsState()
    val showCreateInstanceScreen by routineInstanceViewModel.showCreateScreen.collectAsState()
    val editingInstance by routineInstanceViewModel.editingInstance.collectAsState()
    val preSelectedRoutineId by routineInstanceViewModel.preSelectedRoutineId.collectAsState()
    val expandedInstances by routineInstanceViewModel.expandedInstances.collectAsState()

    var routinesSectionExpanded by remember { mutableStateOf(false) }

    // Show full-screen create experiences when needed
    if (showCreateRoutineScreen) {
        CreateRoutineScreen(
            editingRoutine = editingRoutine,
            onSave = { name, timeIntervals ->
                val routine = editingRoutine
                if (routine != null) {
                    routineViewModel.updateRoutine(routine, name, timeIntervals) {
                        routineInstanceViewModel.refreshRoutineInstancesAfterRoutineUpdate(routine.id)
                    }
                } else {
                    routineViewModel.createRoutine(name, timeIntervals) {
                        routineInstanceViewModel.refreshAvailableRoutines()
                    }
                }
            },
            onCancel = { routineViewModel.hideCreateScreen() }
        )
        return
    }
    
    if (showCreateInstanceScreen) {
        CreateRoutineInstanceScreen(
            availableRoutines = availableRoutines,
            editingInstance = editingInstance,
            preSelectedRoutineId = preSelectedRoutineId,
            onSave = { routineId, name, startTime, daysOfWeek ->
                val instance = editingInstance
                if (instance != null) {
                    routineInstanceViewModel.updateRoutineInstance(instance, routineId, name, startTime, daysOfWeek)
                } else {
                    routineInstanceViewModel.createRoutineInstance(routineId, name, startTime, daysOfWeek)
                }
            },
            onCancel = { routineInstanceViewModel.hideCreateScreen() }
        )
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Routines") }
                )
                HorizontalDivider()
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Routine Instances Section (Always visible)
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Schedule",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (!routineInstances.isEmpty()) {
                                Text(
                                    text = "${routineInstances.size} instances",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { routineInstanceViewModel.showCreateScreen() }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Add")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isLoadingInstances) {
                        CircularProgressIndicator()
                    } else if (routineInstances.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No schedules yet",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Create a routine first, then add schedules to run them at specific times",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Routine Instances List
            items(routineInstances) { routineInstanceWithRoutine ->
                RoutineInstanceCard(
                    routineInstanceWithRoutine = routineInstanceWithRoutine,
                    isExpanded = expandedInstances.contains(routineInstanceWithRoutine.instance.id),
                    onToggle = { routineInstanceViewModel.toggleRoutineInstance(it.instance) },
                    onEdit = { routineInstanceViewModel.startEditingInstance(it.instance) },
                    onDelete = { routineInstanceViewModel.deleteRoutineInstance(it.instance) },
                    onExpandToggle = { instanceId -> routineInstanceViewModel.toggleInstanceExpansion(instanceId) }
                )
            }

            // Routines Section (Collapsible)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
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
                                    text = "Routine Templates",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!routines.isEmpty()) {
                                    Text(
                                        text = "${routines.size} routines",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Row {
                                OutlinedButton(
                                    onClick = { routineViewModel.showCreateScreen() }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text("Add")
                                }
                                
                                IconButton(
                                    onClick = { routinesSectionExpanded = !routinesSectionExpanded }
                                ) {
                                    Icon(
                                        imageVector = if (routinesSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (routinesSectionExpanded) "Collapse" else "Expand"
                                    )
                                }
                            }
                        }
                        
                        AnimatedVisibility(
                            visible = routinesSectionExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (isLoadingRoutines) {
                                    CircularProgressIndicator()
                                } else if (routines.isEmpty()) {
                                    Text(
                                        text = "No routines yet. Create your first routine template!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        routines.forEach { routine ->
                                            RoutineCard(
                                                routine = routine,
                                                onEdit = { routineViewModel.startEditingRoutine(it) },
                                                onDelete = { 
                                                    routineViewModel.deleteRoutine(it) {
                                                        routineInstanceViewModel.refreshAvailableRoutines()
                                                    }
                                                },
                                                onDuplicate = { routine, newName -> 
                                                    routineViewModel.duplicateRoutine(routine, newName) {
                                                        routineInstanceViewModel.refreshAvailableRoutines()
                                                    }
                                                },
                                                onCreateInstance = { routine ->
                                                    routineInstanceViewModel.showCreateScreenWithRoutine(routine.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error Messages
            routineError?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LaunchedEffect(message) {
                        kotlinx.coroutines.delay(3000)
                        routineViewModel.clearErrorMessage()
                    }
                }
            }

            instanceError?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LaunchedEffect(message) {
                        kotlinx.coroutines.delay(3000)
                        routineInstanceViewModel.clearErrorMessage()
                    }
                }
            }
        }
    }
}