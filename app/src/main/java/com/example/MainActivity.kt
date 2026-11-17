package com.example

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.OneTapBoostDialog
import com.example.ui.navigation.AppRoutes
import com.example.ui.navigation.topLevelNavItems
import com.example.ui.screens.CacheRamScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceSettingsScreen
import com.example.ui.screens.FpsNetworkScreen
import com.example.ui.screens.GameSelectionScreen
import com.example.ui.screens.ProfilesHistoryScreen
import com.example.ui.screens.ThermalMonitorScreen
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FFProOptimizerTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.OptimizerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: OptimizerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ObsidianBlack.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(ObsidianSurface.toArgb())
        )

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        viewModel.registerSustainedModeHandler { enable ->
            if (powerManager.isSustainedPerformanceModeSupported) {
                runCatching { window.setSustainedPerformanceMode(enable) }
            }
        }

        setContent {
            FFProOptimizerTheme {
                FFProOptimizerApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshGames()
        viewModel.refreshTelemetryOnce(measurePing = false)
        viewModel.loadBackgroundApps()
    }
}

@Composable
fun FFProOptimizerApp(viewModel: OptimizerViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val profilesList by viewModel.profilesList.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.ROUTE_DASHBOARD
    val baseRoute = currentRoute.substringBefore("?")

    val activeProfile = remember(profilesList, uiState.activeProfileId) {
        profilesList.find { it.id == uiState.activeProfileId } ?: profilesList.firstOrNull()
    }

    LaunchedEffect(uiState.statusSnackbarMessage) {
        val msg = uiState.statusSnackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val showTopLevelNav = baseRoute in setOf(
        AppRoutes.ROUTE_DASHBOARD,
        AppRoutes.ROUTE_CACHE_RAM,
        AppRoutes.ROUTE_THERMAL,
        AppRoutes.ROUTE_FPS_NETWORK
    )

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val ambientBackground = Brush.radialGradient(
        colors = listOf(
            ElectricBlue.copy(alpha = 0.08f),
            NeonGreen.copy(alpha = 0.04f),
            ObsidianBlack
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .background(ambientBackground)
    ) {
        val useRail = maxWidth >= 660.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface.copy(alpha = 0.96f),
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        topLevelNavItems.forEach { item ->
                            val selected = baseRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonGreen,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen.copy(alpha = 0.14f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface.copy(alpha = 0.95f),
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Spacer(modifier = Modifier.weight(0.2f))
                        topLevelNavItems.forEach { item ->
                            val selected = baseRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonGreen,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen.copy(alpha = 0.14f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("${item.testTag}_rail")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.ROUTE_DASHBOARD
                    ) {
                        composable(AppRoutes.ROUTE_DASHBOARD) {
                            DashboardScreen(
                                uiState = uiState,
                                activeProfile = activeProfile,
                                onStartBoost = { viewModel.startOneTapGameBoost() },
                                onOpenGameSelection = { navController.navigate(AppRoutes.ROUTE_GAME_SELECTION) },
                                onOpenCacheCleaner = {
                                    navController.navigate("${AppRoutes.ROUTE_CACHE_RAM}?tab=0")
                                },
                                onOpenRamAnalyzer = {
                                    navController.navigate("${AppRoutes.ROUTE_CACHE_RAM}?tab=1")
                                },
                                onOpenThermalMonitor = { navigateToTopLevel(AppRoutes.ROUTE_THERMAL) },
                                onOpenFpsMonitor = { navigateToTopLevel(AppRoutes.ROUTE_FPS_NETWORK) },
                                onOpenGameSettings = { navController.navigate(AppRoutes.ROUTE_DEVICE_SETTINGS) },
                                onOpenProfilesHistory = { navController.navigate(AppRoutes.ROUTE_PROFILES_HISTORY) },
                                onToggleSessionTimer = {
                                    if (uiState.sessionRunning) viewModel.stopAndResetSessionTimer()
                                    else viewModel.startSessionTimer()
                                }
                            )
                        }

                        composable(AppRoutes.ROUTE_GAME_SELECTION) {
                            GameSelectionScreen(
                                uiState = uiState,
                                onSelectGame = { viewModel.selectGame(it) },
                                onAddManualGame = { viewModel.addAndSelectManualGame(it) },
                                onLoadManualPicker = { viewModel.loadAllAppsForManualPicker() },
                                onRefreshGames = { viewModel.refreshGames() },
                                onStartOptimization = { viewModel.startOneTapGameBoost() },
                                onInstallGame = { viewModel.openPlayStoreForTarget() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "${AppRoutes.ROUTE_CACHE_RAM}?tab={tab}",
                            arguments = listOf(
                                navArgument("tab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) { backStackEntry ->
                            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                            CacheRamScreen(
                                uiState = uiState,
                                initialTab = tab,
                                onScanCache = { viewModel.scanCache() },
                                onCleanCache = { viewModel.cleanAccessibleCache() },
                                onSetSafFolderUri = { viewModel.setSafTreeUri(it) },
                                onOpenSystemCacheCleaner = {
                                    runCatching { context.startActivity(viewModel.buildSystemCacheIntent()) }
                                },
                                onOpenGameStorageSettings = {
                                    viewModel.buildTargetGameStorageIntent()?.let { intent ->
                                        runCatching { context.startActivity(intent) }
                                    }
                                },
                                onOptimizeRamSafely = { viewModel.optimizeRamSafely() },
                                onRefreshBackgroundApps = { viewModel.loadBackgroundApps() },
                                onOpenUsageAccessSettings = {
                                    runCatching {
                                        context.startActivity(viewModel.repository.backgroundActivity.buildUsageAccessSettingsIntent())
                                    }
                                },
                                onOpenAppInfo = { pkg ->
                                    runCatching {
                                        context.startActivity(viewModel.repository.backgroundActivity.buildAppInfoIntent(pkg))
                                    }
                                }
                            )
                        }

                        composable(AppRoutes.ROUTE_THERMAL) {
                            ThermalMonitorScreen(
                                uiState = uiState,
                                onToggleAlerts = { viewModel.setOverheatingAlertsEnabled(it) },
                                onChangeThreshold = { viewModel.setThermalThreshold(it) },
                                onToggleFahrenheit = { viewModel.setUseFahrenheit(it) },
                                onOpenBackgroundMonitor = {
                                    navController.navigate("${AppRoutes.ROUTE_CACHE_RAM}?tab=1")
                                }
                            )
                        }

                        composable(AppRoutes.ROUTE_FPS_NETWORK) {
                            FpsNetworkScreen(
                                uiState = uiState,
                                onStartFpsTracking = { viewModel.startFpsMonitoring() },
                                onStopFpsTracking = { viewModel.stopFpsMonitoring() },
                                onResetFrameDrops = { viewModel.resetFrameDrops() },
                                onToggleSustainedMode = { viewModel.setSustainedPerformanceEnabled(it) },
                                onToggleGamingDnd = { viewModel.setGamingDndEnabled(it) },
                                onOpenDndSettings = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                },
                                onOpenPowerSettings = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                },
                                onSelectEndpoint = { viewModel.selectEndpoint(it) },
                                onRunStabilityTest = { viewModel.runConnectionStabilityTest() }
                            )
                        }

                        composable(AppRoutes.ROUTE_PROFILES_HISTORY) {
                            ProfilesHistoryScreen(
                                profiles = profilesList,
                                history = historyList,
                                activeProfileId = uiState.activeProfileId,
                                useFahrenheit = uiState.useFahrenheit,
                                onSelectProfile = { viewModel.selectProfile(it) },
                                onCreateProfile = { name, desc, cache, sust, dnd, net, temp, preset ->
                                    viewModel.createCustomProfile(name, desc, cache, sust, dnd, net, temp, preset)
                                },
                                onDeleteCustomProfile = { viewModel.deleteCustomProfile(it) },
                                onClearHistory = { viewModel.clearHistory() },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppRoutes.ROUTE_DEVICE_SETTINGS) {
                            DeviceSettingsScreen(
                                uiState = uiState,
                                onToggleFahrenheit = { viewModel.setUseFahrenheit(it) },
                                onToggleSustainedPerformance = { viewModel.setSustainedPerformanceEnabled(it) },
                                onToggleOverheatingAlerts = { viewModel.setOverheatingAlertsEnabled(it) },
                                onStartOrResetTimer = {
                                    if (uiState.sessionRunning) viewModel.stopAndResetSessionTimer()
                                    else viewModel.startSessionTimer()
                                },
                                onOpenUsageAccess = {
                                    runCatching {
                                        context.startActivity(viewModel.repository.backgroundActivity.buildUsageAccessSettingsIntent())
                                    }
                                },
                                onOpenDndAccess = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                },
                                onOpenProfilesHistory = { navController.navigate(AppRoutes.ROUTE_PROFILES_HISTORY) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showBoostDialog) {
        OneTapBoostDialog(
            steps = uiState.boostSteps,
            isRunning = uiState.isBoostInProgress,
            summary = uiState.boostSummary,
            useFahrenheit = uiState.useFahrenheit,
            onDismiss = { viewModel.dismissBoostDialog() },
            onLaunchGame = { viewModel.launchTargetGameFromBoost() },
            onInstallGame = { viewModel.openPlayStoreForTarget() }
        )
    }
}
