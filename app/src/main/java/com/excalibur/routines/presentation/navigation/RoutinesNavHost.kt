package com.excalibur.routines.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.excalibur.routines.presentation.routineeditor.RoutineEditorScreen
import com.excalibur.routines.presentation.routineeditor.RoutineEditorViewModel
import com.excalibur.routines.presentation.routinelist.RoutineListScreen
import com.excalibur.routines.presentation.routinelist.RoutineListViewModel
import com.excalibur.routines.presentation.scheduleeditor.ScheduleEditorScreen
import com.excalibur.routines.presentation.scheduleeditor.ScheduleEditorViewModel
import com.excalibur.routines.presentation.session.ActiveSessionScreen
import com.excalibur.routines.presentation.session.ActiveSessionViewModel

@Composable
fun RoutinesNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RoutineList,
        modifier = modifier
    ) {
        composable<Screen.RoutineList> {
            val viewModel: RoutineListViewModel = hiltViewModel()
            RoutineListScreen(
                viewModel = viewModel,
                onCreateRoutine = { navController.navigate(Screen.RoutineEditor()) },
                onEditRoutine = { routineId -> navController.navigate(Screen.RoutineEditor(routineId = routineId)) },
                onAddSchedule = { routineId -> navController.navigate(Screen.ScheduleEditor(routineId = routineId)) },
                onEditSchedule = { routineId, scheduleId ->
                    navController.navigate(Screen.ScheduleEditor(routineId = routineId, scheduleId = scheduleId))
                },
                onStartRoutine = { routineId ->
                    navController.navigate(Screen.ActiveSession(routineId = routineId))
                }
            )
        }

        composable<Screen.RoutineEditor> {
            val viewModel: RoutineEditorViewModel = hiltViewModel()
            RoutineEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ScheduleEditor> {
            val viewModel: ScheduleEditorViewModel = hiltViewModel()
            ScheduleEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ActiveSession> {
            val viewModel: ActiveSessionViewModel = hiltViewModel()
            ActiveSessionScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    val popped = navController.popBackStack(Screen.RoutineList, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.RoutineList) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
