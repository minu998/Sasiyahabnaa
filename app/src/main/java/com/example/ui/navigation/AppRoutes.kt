package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector

object AppRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_GAME_SELECTION = "game_selection"
    const val ROUTE_CACHE_RAM = "cache_ram"
    const val ROUTE_THERMAL = "thermal_monitor"
    const val ROUTE_FPS_NETWORK = "fps_network"
    const val ROUTE_PROFILES_HISTORY = "profiles_history"
    const val ROUTE_DEVICE_SETTINGS = "device_settings"
}

data class TopLevelNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val topLevelNavItems = listOf(
    TopLevelNavItem(
        route = AppRoutes.ROUTE_DASHBOARD,
        label = "Dashboard",
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    TopLevelNavItem(
        route = AppRoutes.ROUTE_CACHE_RAM,
        label = "Cache & RAM",
        selectedIcon = Icons.Rounded.CleaningServices,
        unselectedIcon = Icons.Outlined.CleaningServices,
        testTag = "nav_cache_ram"
    ),
    TopLevelNavItem(
        route = AppRoutes.ROUTE_THERMAL,
        label = "Thermal",
        selectedIcon = Icons.Rounded.Thermostat,
        unselectedIcon = Icons.Outlined.Thermostat,
        testTag = "nav_thermal"
    ),
    TopLevelNavItem(
        route = AppRoutes.ROUTE_FPS_NETWORK,
        label = "FPS & Net",
        selectedIcon = Icons.Rounded.Speed,
        unselectedIcon = Icons.Outlined.Speed,
        testTag = "nav_fps_net"
    )
)
