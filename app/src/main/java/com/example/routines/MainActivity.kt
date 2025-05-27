package com.example.routines

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast


import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AlarmScheduler(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                )// Handles system bars
            }
            createNotificationChannel()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Channel"
            val descriptionText = "Channel for Alarm manager"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarm_channel_id", name, importance).apply { description = descriptionText }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission granted")
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied")
                Toast.makeText(this, "Notification permission denied. Alarms may not show notifications.", Toast.LENGTH_LONG).show()
            }
        }
}

@Composable
fun AlarmScheduler(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val timePickerDialogState = remember { mutableStateOf(false) }
    val alarmList = remember { mutableStateListOf<String>() }
    var selectedTime by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // App doesn't have permission — prompt user
                val context = LocalContext.current
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            } else {
                // App can schedule exact alarms
            }
        } else {
            // Below Android 12 — permission not required
        }

        Button(onClick = { timePickerDialogState.value = true }) {
            Text("Select Time")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTime.isNotEmpty()) {
            Text("Selected Time: $selectedTime")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (selectedTime.isNotEmpty() && !alarmList.contains(selectedTime)) {
                alarmList.add(selectedTime)
                // Optionally, schedule the alarm here or when a "Set Alarms" button is pressed
            }
        }) {
            Text("Add Alarm Time")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Scheduled Alarms:")
        LazyColumn {
            items(alarmList) { time ->
                Text(time, modifier = Modifier.padding(4.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes button to the bottom

        Button(onClick = {
            alarmList.forEach { timeString ->
                setAlarm(context, timeString)
            }
        }) {
            Text("Ring Alarms Now (Test)")
        }
    }

    ShowTimePicker(context, timePickerDialogState, onTimeSelected = { time -> selectedTime = time })
}

@Composable
fun ShowTimePicker(
    context: Context,
    showDialog: MutableState<Boolean>,
    onTimeSelected: (String) -> Unit
) {
    if (showDialog.value) {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                onTimeSelected(String.format(Locale.US,"%02d:%02d", hour, minute))
                showDialog.value = false
            },
            12, 0, true // Initial hour, minute, and 24-hour format
        )
        timePickerDialog.setOnDismissListener { showDialog.value = false }
        timePickerDialog.show()
    }
}

fun setAlarm(context: Context, timeString: String) {
    val parts = timeString.split(":")
    if (parts.size != 2) {
        Log.e("AlarmScheduler", "Invalid time format: $timeString")
        Toast.makeText(context, "Invalid time format: $timeString", Toast.LENGTH_SHORT).show()
        return
    }

    val hour = parts[0].toIntOrNull()
    val minute = parts[1].toIntOrNull()

    if (hour == null || minute == null) {
        Log.e("AlarmScheduler", "Invalid time numbers: $timeString")
        Toast.makeText(context, "Invalid time numbers: $timeString", Toast.LENGTH_SHORT).show()
        return
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(
        context,
        AlarmReceiver::class.java
    ).apply {
        putExtra("ALARM_TIME", timeString) // Optional: pass data to receiver
    }

    // Use a unique request code for each alarm to avoid conflicts
    val requestCode = timeString.hashCode() // Simple way to generate a somewhat unique code
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        // If the time is in the past, set it for the next day
        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }
    }

    try {
        // Use setExactAndAllowWhileIdle for more precise alarms, especially on newer Android versions
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Log.d("AlarmScheduler", "Alarm set for $timeString at ${calendar.time}")
        Toast.makeText(context, "Alarm set for $timeString", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.e("AlarmScheduler", "SecurityException: Missing SCHEDULE_EXACT_ALARM permission?", e)
        Toast.makeText(context, "Error setting alarm. Check permissions.", Toast.LENGTH_LONG).show()
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")
        // Toast.makeText(context, "Alarm ringing!", Toast.LENGTH_LONG).show()

        val alarmTime = intent.getStringExtra("ALARM_TIME") ?: "Alarm"

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra("ALARM_TIME", alarmTime)
        }

        // Start the foreground service
        // For Android O and above, you must use startForegroundService and then
        // the service must call startForeground() within 5 seconds.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        /*
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "alarm_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .setContentTitle("Alarm")
            .setContentText("It's time! ($alarmTime)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        // Use a unique notification ID, e.g., based on the alarm time or a static ID if only one notification type
        notificationManager.notify(alarmTime.hashCode(), notification)

        // Here you would typically play a sound, vibrate, or show a full-screen activity
        */
    }
}
