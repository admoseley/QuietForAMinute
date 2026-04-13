package com.admoseley.quietforaminute.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.admoseley.quietforaminute.ui.schedules.ScheduleEditScreen
import com.admoseley.quietforaminute.ui.schedules.ScheduleListScreen
import com.admoseley.quietforaminute.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Schedules : Screen("schedules", "Schedules", Icons.Default.CalendarMonth)
    object ScheduleEdit : Screen("schedules/edit?id={id}", "Edit Schedule", Icons.Default.CalendarMonth) {
        fun route(id: Long = -1L) = "schedules/edit?id=$id"
    }
}

val bottomNavItems = listOf(Screen.Settings, Screen.Schedules)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Settings.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.Schedules.route) {
                ScheduleListScreen(
                    onAddSchedule = { navController.navigate(Screen.ScheduleEdit.route()) },
                    onEditSchedule = { id -> navController.navigate(Screen.ScheduleEdit.route(id)) }
                )
            }
            composable(
                route = Screen.ScheduleEdit.route,
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: -1L
                ScheduleEditScreen(
                    scheduleId = id,
                    onSaved = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
