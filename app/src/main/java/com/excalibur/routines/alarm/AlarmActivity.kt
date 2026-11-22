package com.excalibur.routines.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.excalibur.routines.ui.theme.RoutinesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        turnScreenOnAndDismissKeyguard()

        // Keep screen awake while this alert activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val routineName = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_ROUTINE_NAME) ?: "Routine Alarm"
        val routineIcon = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_ROUTINE_ICON) ?: "🦊"
        val stepName = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_STEP_NAME) ?: "Step"
        val stepIndex = intent.getIntExtra(AlarmSchedulerImpl.EXTRA_STEP_INDEX, 0)
        val totalSteps = intent.getIntExtra(AlarmSchedulerImpl.EXTRA_TOTAL_STEPS, 1)
        val nextStepName = intent.getStringExtra(AlarmSchedulerImpl.EXTRA_NEXT_STEP_NAME)
        val isCompletion = intent.getBooleanExtra(AlarmSchedulerImpl.EXTRA_IS_COMPLETION, false)

        setContent {
            RoutinesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmRingingContent(
                        routineName = routineName,
                        routineIcon = routineIcon,
                        stepName = stepName,
                        stepIndex = stepIndex,
                        totalSteps = totalSteps,
                        nextStepName = nextStepName,
                        isCompletion = isCompletion,
                        onDismiss = {
                            val stopIntent = Intent(this, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_STOP_ALARM
                            }
                            startService(stopIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun turnScreenOnAndDismissKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }
}

@Composable
fun AlarmRingingContent(
    routineName: String,
    routineIcon: String,
    stepName: String,
    stepIndex: Int,
    totalSteps: Int,
    nextStepName: String?,
    isCompletion: Boolean,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding() // Ensures bottom button is never occluded by system navigation bar!
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Animal Avatar & Routine title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCompletion) "🎉" else routineIcon,
                    fontSize = 52.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = routineName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isCompletion) "All tasks finished!" else "Step ${stepIndex + 1} of $totalSteps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Current Step / Completion Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isCompletion) "CELEBRATION" else "CURRENT TASK",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stepName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                if (!nextStepName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Next: $nextStepName",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Action Button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = when {
                    isCompletion -> "Done! Great Job"
                    stepIndex + 1 < totalSteps -> "Got it! Start Step"
                    else -> "Start Final Step"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
