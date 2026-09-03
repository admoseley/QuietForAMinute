package com.admoseley.quietforaminute.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.admoseley.quietforaminute.R
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

sealed class Screen(val route: String, val label: String, @param:DrawableRes val icon: Int) {
    object Settings : Screen("settings", "Settings", R.drawable.ic_settings)
    object Schedules : Screen("schedules", "Schedules", R.drawable.ic_calendar_month)
    object ScheduleEdit : Screen("schedules/edit?id={id}", "Edit Schedule", R.drawable.ic_calendar_month) {
        fun route(id: Long = -1L) = "schedules/edit?id=$id"
    }
}

val bottomNavItems = listOf(Screen.Settings, Screen.Schedules)

/** SavedStateHandle key used to pass the "saved without alarms armed" result back to the list. */
private const val KEY_ALARMS_NOT_ARMED = "alarmsNotArmed"

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
            composable(Screen.Schedules.route) { backStackEntry ->
                // Set by ScheduleEditScreen's onSaved below, via the *previous* back stack
                // entry's savedStateHandle — the standard Navigation Compose way to pass a
                // one-shot result back after a pop, since these two screens have separate
                // ViewModels and can't share a SharedFlow directly.
                val alarmsNotArmed by backStackEntry.savedStateHandle
                    .getStateFlow(KEY_ALARMS_NOT_ARMED, false)
                    .collectAsState()
                ScheduleListScreen(
                    justSavedWithoutAlarms = alarmsNotArmed,
                    onJustSavedWithoutAlarmsConsumed = {
                        backStackEntry.savedStateHandle[KEY_ALARMS_NOT_ARMED] = false
                    },
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
                    onSaved = { alarmsArmed ->
                        if (!alarmsArmed) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(KEY_ALARMS_NOT_ARMED, true)
                        }
                        navController.popBackStack()
                    },
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
                icon = { Icon(painterResource(screen.icon), contentDescription = screen.label) },
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
