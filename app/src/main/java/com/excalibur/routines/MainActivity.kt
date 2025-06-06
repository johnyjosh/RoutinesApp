package com.excalibur.routines

import android.app.AlarmManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.excalibur.routines.data.managers.AppNotificationManager
import com.excalibur.routines.data.managers.PermissionManager
import com.excalibur.routines.data.repositories.AndroidAlarmRepository
import com.excalibur.routines.data.repositories.AndroidRoutineRepository
import com.excalibur.routines.data.repositories.AndroidRoutineInstanceRepository
import com.excalibur.routines.presentation.screens.RoutinesMainScreen
import com.excalibur.routines.presentation.viewmodels.AlarmViewModel
import com.excalibur.routines.presentation.viewmodels.RoutineViewModel
import com.excalibur.routines.presentation.viewmodels.RoutineInstanceViewModel
import com.excalibur.routines.domain.services.RoutineAlarmScheduler

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    private lateinit var notificationManager: AppNotificationManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionManager = PermissionManager(this)
        notificationManager = AppNotificationManager(this)
        
        checkAndRequestPermissions()
        
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val sharedPreferences = getSharedPreferences("routines_prefs", Context.MODE_PRIVATE)
                
                // Repositories
                val alarmRepository = AndroidAlarmRepository(this@MainActivity, alarmManager, sharedPreferences)
                val routineRepository = AndroidRoutineRepository(sharedPreferences)
                val routineInstanceRepository = AndroidRoutineInstanceRepository(sharedPreferences)
                
                // Services
                val routineAlarmScheduler = RoutineAlarmScheduler(alarmRepository)
                
                // ViewModels
                val routineViewModel: RoutineViewModel = viewModel(
                    factory = RoutineViewModel.Factory(routineRepository, routineInstanceRepository, routineAlarmScheduler)
                )
                
                val routineInstanceViewModel: RoutineInstanceViewModel = viewModel(
                    factory = RoutineInstanceViewModel.Factory(routineInstanceRepository, routineRepository, routineAlarmScheduler)
                )
                
                RoutinesMainScreen(
                    routineViewModel = routineViewModel,
                    routineInstanceViewModel = routineInstanceViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!permissionManager.hasNotificationPermission()) {
            Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
            requestPermissionLauncher.launch(permissionManager.getNotificationPermission())
        }
        
        if (!permissionManager.canScheduleExactAlarms()) {
            val intent = permissionManager.getExactAlarmPermissionIntent()
            startActivity(intent)
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

