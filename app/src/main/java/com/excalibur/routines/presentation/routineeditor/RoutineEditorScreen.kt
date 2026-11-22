package com.excalibur.routines.presentation.routineeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.excalibur.routines.domain.model.RoutineAvatar
import com.excalibur.routines.domain.model.RoutineStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    viewModel: RoutineEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddStepDialog by remember { mutableStateOf(false) }
    var editingStep by remember { mutableStateOf<RoutineStep?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "New Routine" else "Edit Routine") },
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
                        IconButton(
                            onClick = { viewModel.save(onSuccess = onNavigateBack) },
                            enabled = uiState.name.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animal Avatar Picker Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Routine Companion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoutineAvatar.entries.forEach { avatar ->
                                val isSelected = uiState.icon == avatar.emoji
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { viewModel.updateIcon(avatar.emoji) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = avatar.emoji,
                                        fontSize = if (isSelected) 30.sp else 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Routine Name *") },
                    placeholder = { Text("e.g. Morning Workout") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Quick notes or focus points") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }

            // Steps Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Steps (${uiState.steps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showAddStepDialog = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Step")
                    }
                }
            }

            if (uiState.steps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No steps added yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add timed intervals like '10m stretch', '20m shower' to build your routine sequence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(uiState.steps, key = { _, step -> step.id }) { index, step ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingStep = step },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Grip Handle
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(end = 10.dp)
                                )

                                Column {
                                    Text(
                                        text = "${index + 1}. ${step.name}",
                                        style = MaterialTheme.typography.bodyLarge,
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
                                                text = step.formattedDuration,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = if (step.isForcefulAlarm) "🚨 Alarm" else "🔔 Chime",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editingStep = step }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Step", modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.moveStep(index, index - 1) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.moveStep(index, index + 1) },
                                    enabled = index < uiState.steps.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.removeStep(step.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
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

    if (showAddStepDialog) {
        StepEditorDialog(
            initialStep = null,
            onDismiss = { showAddStepDialog = false },
            onConfirm = { name, mins, secs, isForceful ->
                viewModel.addStep(name, mins, secs, isForceful)
                showAddStepDialog = false
            }
        )
    }

    editingStep?.let { step ->
        StepEditorDialog(
            initialStep = step,
            onDismiss = { editingStep = null },
            onConfirm = { name, mins, secs, isForceful ->
                viewModel.updateStep(step.id, name, mins, secs, isForceful)
                editingStep = null
            }
        )
    }
}

@Composable
fun StepEditorDialog(
    initialStep: RoutineStep? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, minutes: Int, seconds: Int, isForceful: Boolean) -> Unit
) {
    var stepName by remember(initialStep) { mutableStateOf(initialStep?.name ?: "") }
    var minutesText by remember(initialStep) {
        mutableStateOf(initialStep?.durationMinutes?.toString() ?: "10")
    }
    var secondsText by remember(initialStep) {
        mutableStateOf(initialStep?.durationSeconds?.toString() ?: "0")
    }
    var isForceful by remember(initialStep) {
        mutableStateOf(initialStep?.isForcefulAlarm ?: true)
    }

    val isEditing = initialStep != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Step" else "Add Routine Step") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = stepName,
                    onValueChange = { stepName = it },
                    label = { Text("Step Name") },
                    placeholder = { Text("e.g. Stretching") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter { char -> char.isDigit() } },
                        label = { Text("Seconds") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Alert Style Choice: Forceful Alarm vs Quick Chime
                Text(
                    text = "Reminder Strength",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isForceful,
                        onClick = { isForceful = true },
                        label = { Text("🚨 Forceful Alarm") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isForceful,
                        onClick = { isForceful = false },
                        label = { Text("🔔 Quick Chime") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = minutesText.toIntOrNull() ?: 0
                    val secs = secondsText.toIntOrNull() ?: 0
                    if (mins > 0 || secs > 0) {
                        onConfirm(stepName, mins, secs, isForceful)
                    }
                },
                enabled = (minutesText.toIntOrNull() ?: 0) > 0 || (secondsText.toIntOrNull() ?: 0) > 0
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
