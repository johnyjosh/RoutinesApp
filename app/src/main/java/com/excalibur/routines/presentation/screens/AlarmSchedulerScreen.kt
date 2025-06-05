package com.excalibur.routines.presentation.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.excalibur.routines.domain.models.AlarmItem
import com.excalibur.routines.presentation.viewmodels.AlarmViewModel
import java.util.Locale

@Composable
fun AlarmSchedulerScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsState()
    val showTimePicker by viewModel.showTimePicker.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { viewModel.showTimePickerForNewAlarm() }) {
            Text("Add Alarm Time")
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearErrorMessage()
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Scheduled Alarms:", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(alarms) { alarm ->
                AlarmItemCard(
                    alarm = alarm,
                    onToggle = { viewModel.toggleAlarm(alarm) },
                    onRemove = { viewModel.removeAlarm(alarm) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(onClick = { viewModel.testAlarms() }) {
            Text("Ring Alarms Now (Test)")
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            context = context,
            onTimeSelected = { hour, minute ->
                viewModel.onTimeSelectedAndAddAlarm(hour, minute)
            },
            onDismiss = { viewModel.hideTimePicker() }
        )
    }
}

@Composable
fun AlarmItemCard(
    alarm: AlarmItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = alarm.getTimeString(),
                    style = MaterialTheme.typography.titleMedium
                )
                if (alarm.title.isNotEmpty()) {
                    Text(
                        text = alarm.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                
                Spacer(modifier = Modifier.padding(8.dp))
                
                OutlinedButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    context: Context,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            onTimeSelected(hour, minute)
        },
        12, 0, false  // Changed to false for 12-hour format
    )
    
    timePickerDialog.setOnDismissListener { onDismiss() }
    
    LaunchedEffect(Unit) {
        timePickerDialog.show()
    }
}