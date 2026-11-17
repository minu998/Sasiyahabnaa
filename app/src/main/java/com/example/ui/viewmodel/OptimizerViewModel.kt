package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.OptimizationHistoryEntity
import com.example.data.local.OptimizationProfileEntity
import com.example.data.model.BackgroundAppEntry
import com.example.data.model.BoostStep
import com.example.data.model.BoostStepStatus
import com.example.data.model.CacheScanResult
import com.example.data.model.DeviceHardwareSpec
import com.example.data.model.DeviceMetricsSnapshot
import com.example.data.model.GameEntry
import com.example.data.model.OptimizationSummary
import com.example.data.model.PingEndpoint
import com.example.data.model.PingTestReport
import com.example.data.model.ThermalLevel
import com.example.data.repository.OptimizerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OptimizerUiState(
    val metrics: DeviceMetricsSnapshot = DeviceMetricsSnapshot(),
    val primaryGames: List<GameEntry> = emptyList(),
    val detectedGames: List<GameEntry> = emptyList(),
    val allAppsForPicker: List<GameEntry> = emptyList(),
    val selectedGame: GameEntry? = null,
    val cacheScan: CacheScanResult = CacheScanResult(),
    val isScanningCache: Boolean = false,
    val isCleaningCache: Boolean = false,
    val backgroundApps: List<BackgroundAppEntry> = emptyList(),
    val isLoadingBackgroundApps: Boolean = false,
    val isOptimizingRam: Boolean = false,
    val ramOptimizationMessage: String? = null,
    val endpoints: List<PingEndpoint> = emptyList(),
    val selectedEndpoint: PingEndpoint? = null,
    val pingReport: PingTestReport? = null,
    val livePingSamples: List<Int> = emptyList(),
    val liveFpsSamples: List<Float> = emptyList(),
    val liveTempSamples: List<Float> = emptyList(),
    val isRunningPingTest: Boolean = false,
    val pingTestProgressSamples: List<Int?> = emptyList(),
    val hardwareSpec: DeviceHardwareSpec? = null,
    val activeProfileId: Long = 1L,
    val useFahrenheit: Boolean = false,
    val overheatingAlertsEnabled: Boolean = true,
    val thermalThresholdCelsius: Float = 40.0f,
    val sustainedPerformanceEnabled: Boolean = true,
    val gamingDndEnabled: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val sessionRunning: Boolean = false,
    val sessionElapsedMs: Long = 0L,
    // One-tap boost modal state
    val isBoostInProgress: Boolean = false,
    val showBoostDialog: Boolean = false,
    val boostSteps: List<BoostStep> = emptyList(),
    val boostSummary: OptimizationSummary? = null,
    val statusSnackbarMessage: String? = null
)

class OptimizerViewModel(application: Application) : AndroidViewModel(application) {

    val repository = OptimizerRepository(application)

    private val _uiState = MutableStateFlow(OptimizerUiState())
    val uiState: StateFlow<OptimizerUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<OptimizationHistoryEntity>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profilesList: StateFlow<List<OptimizationProfileEntity>> = repository.profilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var telemetryJob: Job? = null
    private var boostJob: Job? = null
    private var sustainedModeCallback: ((Boolean) -> Unit)? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfilesSeeded()
            val endpoints = repository.getEndpoints()
            val savedEndpointId = repository.prefs.selectedPingEndpointId
            val chosenEndpoint = endpoints.find { it.id == savedEndpointId } ?: endpoints.first()

            _uiState.value = _uiState.value.copy(
                endpoints = endpoints,
                selectedEndpoint = chosenEndpoint,
                hardwareSpec = repository.getHardwareSpecs(),
                activeProfileId = repository.prefs.activeProfileId,
                useFahrenheit = repository.prefs.useFahrenheit,
                overheatingAlertsEnabled = repository.prefs.overheatingAlertsEnabled,
                thermalThresholdCelsius = repository.prefs.thermalThresholdCelsius,
                sustainedPerformanceEnabled = repository.prefs.gamingModeSustainedEnabled,
                gamingDndEnabled = repository.prefs.gamingModeDndEnabled,
                hasNotificationPermission = repository.thermalNotifier.hasNotificationPermission(),
                sessionRunning = repository.prefs.sessionStartTimestampMs > 0L
            )

            refreshGames()
            scanCache()
            refreshTelemetryOnce(measurePing = true)
            loadBackgroundApps()
            startTelemetryLoop()
        }
    }

    fun registerSustainedModeHandler(handler: (Boolean) -> Unit) {
        sustainedModeCallback = handler
        handler(_uiState.value.sustainedPerformanceEnabled)
    }

    fun startFpsMonitoring() {
        repository.telemetryMonitor.startFpsTracking()
    }

    fun stopFpsMonitoring() {
        repository.telemetryMonitor.stopFpsTracking()
    }

    fun resetFrameDrops() {
        repository.telemetryMonitor.resetFrameDrops()
        refreshTelemetryOnce(measurePing = false)
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(2000L)
                tick++
                refreshTelemetryOnce(measurePing = tick % 3 == 0)
            }
        }
    }

    fun refreshTelemetryOnce(measurePing: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            var pingMs = state.metrics.lastPingMs
            if (measurePing && state.selectedEndpoint != null) {
                pingMs = repository.pingOnce(state.selectedEndpoint)
            }

            val snapshot = repository.collectSnapshot(
                accessibleCacheBytes = state.cacheScan.totalReclaimableBytes,
                lastPingMs = pingMs,
                pingJitterMs = state.pingReport?.jitterMs ?: state.metrics.pingJitterMs,
                packetLossPercent = state.pingReport?.packetLossPercent ?: state.metrics.packetLossPercent,
                isSustainedActive = state.sustainedPerformanceEnabled && state.metrics.isSustainedPerformanceSupported
            )

            val sessionStart = repository.prefs.sessionStartTimestampMs
            val sessionElapsed = if (sessionStart > 0L) {
                (System.currentTimeMillis() - sessionStart).coerceAtLeast(0L)
            } else 0L

            val updatedFpsHistory = (state.liveFpsSamples + snapshot.liveUiFps).takeLast(24)
            val updatedTempHistory = (state.liveTempSamples + snapshot.batteryTempCelsius).takeLast(24)
            val updatedPingHistory = if (pingMs != null && measurePing) {
                (state.livePingSamples + pingMs).takeLast(20)
            } else state.livePingSamples

            // Automatic overheating alert check
            if (state.overheatingAlertsEnabled &&
                snapshot.batteryTempCelsius >= state.thermalThresholdCelsius
            ) {
                repository.thermalNotifier.maybeNotifyOverheating(
                    snapshot.batteryTempCelsius,
                    state.thermalThresholdCelsius
                )
            }

            _uiState.value = _uiState.value.copy(
                metrics = snapshot,
                liveFpsSamples = updatedFpsHistory,
                liveTempSamples = updatedTempHistory,
                livePingSamples = updatedPingHistory,
                sessionRunning = sessionStart > 0L,
                sessionElapsedMs = sessionElapsed,
                hasNotificationPermission = repository.thermalNotifier.hasNotificationPermission()
            )
        }
    }

    fun refreshGames() {
        viewModelScope.launch {
            val primary = repository.getPrimaryTargets()
            val detected = repository.getDetectedGames()
            val selectedPkg = repository.prefs.selectedGamePackage
            val resolvedSelected = repository.resolveTargetGame(selectedPkg)
            _uiState.value = _uiState.value.copy(
                primaryGames = primary,
                detectedGames = detected,
                selectedGame = resolvedSelected
            )
        }
    }

    fun loadAllAppsForManualPicker() {
        viewModelScope.launch {
            val apps = repository.getAllLaunchableApps()
            _uiState.value = _uiState.value.copy(allAppsForPicker = apps)
        }
    }

    fun selectGame(packageName: String) {
        repository.prefs.selectedGamePackage = packageName
        val resolved = repository.resolveTargetGame(packageName)
        _uiState.value = _uiState.value.copy(selectedGame = resolved)
    }

    fun addAndSelectManualGame(packageName: String) {
        val current = repository.prefs.manualCustomPackages.toMutableSet()
        current.add(packageName)
        repository.prefs.manualCustomPackages = current
        selectGame(packageName)
        refreshGames()
    }

    fun scanCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningCache = true)
            val result = repository.scanCache()
            val prevCleaned = _uiState.value.cacheScan.lastCleanedBytes
            _uiState.value = _uiState.value.copy(
                cacheScan = result.copy(lastCleanedBytes = prevCleaned),
                isScanningCache = false
            )
            refreshTelemetryOnce(measurePing = false)
        }
    }

    fun cleanAccessibleCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaningCache = true)
            val freed = repository.cleanCache()
            val postScan = repository.scanCache()
            _uiState.value = _uiState.value.copy(
                cacheScan = postScan.copy(lastCleanedBytes = freed),
                isCleaningCache = false,
                statusSnackbarMessage = if (freed > 0L) {
                    "Safely cleared ${com.example.util.Formatters.formatBytes(freed)} of accessible cache"
                } else {
                    "Accessible cache is already clean"
                }
            )
            refreshTelemetryOnce(measurePing = false)
        }
    }

    fun setSafTreeUri(uriString: String?) {
        repository.prefs.safCacheTreeUri = uriString
        scanCache()
    }

    fun buildSystemCacheIntent(): Intent = repository.cacheCleaner.buildSystemClearCacheIntent()

    fun buildTargetGameStorageIntent(): Intent? {
        val target = _uiState.value.selectedGame ?: return null
        return if (target.isInstalled) {
            repository.cacheCleaner.buildAppStorageSettingsIntent(target.packageName)
        } else null
    }

    fun loadBackgroundApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingBackgroundApps = true)
            val hasUsage = repository.telemetryMonitor.hasUsageStatsPermission()
            val targetPkg = repository.prefs.selectedGamePackage
            val apps = repository.getBackgroundApps(targetPkg, hasUsage)
            _uiState.value = _uiState.value.copy(
                backgroundApps = apps,
                isLoadingBackgroundApps = false
            )
        }
    }

    fun optimizeRamSafely() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOptimizingRam = true)
            val beforeAvail = _uiState.value.metrics.ramAvailableMb
            System.runFinalization()
            System.gc()
            delay(350L)
            refreshTelemetryOnce(measurePing = false)
            delay(150L)
            val afterAvail = _uiState.value.metrics.ramAvailableMb
            val delta = afterAvail - beforeAvail
            val userBackgroundCount = _uiState.value.backgroundApps.count { !it.isSystemCritical }
            val msg = buildString {
                append("Released optimizer runtime buffers (")
                append(if (delta >= 0) "+$delta MB" else "$delta MB")
                append(" available RAM delta). ")
                if (userBackgroundCount > 0) {
                    append("$userBackgroundCount non-system background apps detected—review below to restrict them safely.")
                } else {
                    append("No heavy non-system background apps flagged.")
                }
            }
            _uiState.value = _uiState.value.copy(
                isOptimizingRam = false,
                ramOptimizationMessage = msg
            )
        }
    }

    fun selectEndpoint(endpoint: PingEndpoint) {
        repository.prefs.selectedPingEndpointId = endpoint.id
        _uiState.value = _uiState.value.copy(selectedEndpoint = endpoint)
        refreshTelemetryOnce(measurePing = true)
    }

    fun runConnectionStabilityTest() {
        val endpoint = _uiState.value.selectedEndpoint ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunningPingTest = true,
                pingTestProgressSamples = emptyList()
            )
            val report = repository.runStabilityTest(endpoint) { partial ->
                _uiState.value = _uiState.value.copy(pingTestProgressSamples = partial)
            }
            _uiState.value = _uiState.value.copy(
                isRunningPingTest = false,
                pingReport = report,
                metrics = _uiState.value.metrics.copy(
                    lastPingMs = report.avgMs ?: _uiState.value.metrics.lastPingMs,
                    pingJitterMs = report.jitterMs,
                    packetLossPercent = report.packetLossPercent
                )
            )
        }
    }

    fun startOneTapGameBoost() {
        if (_uiState.value.isBoostInProgress) return
        val targetGame = _uiState.value.selectedGame ?: repository.resolveTargetGame(repository.prefs.selectedGamePackage)
        val activeProfile = profilesList.value.find { it.id == _uiState.value.activeProfileId }
            ?: profilesList.value.firstOrNull()

        val initialSteps = listOf(
            BoostStep(1, "Check Device Performance", "Evaluating CPU, display refresh rate, and system load...", BoostStepStatus.RUNNING),
            BoostStep(2, "Check Available RAM", "Measuring free memory headroom & runtime heap...", BoostStepStatus.PENDING),
            BoostStep(3, "Check Battery Temperature", "Reading battery sensor & PowerManager thermal status...", BoostStepStatus.PENDING),
            BoostStep(4, "Analyze Accessible Temp Files", "Scanning safe cache & temporary buffers...", BoostStepStatus.PENDING),
            BoostStep(5, "Generate Safe Recommendations", "Preparing device-specific Free Fire presets...", BoostStepStatus.PENDING),
            BoostStep(6, "Apply Supported Performance Settings", "Configuring Sustained Performance & Gaming DND...", BoostStepStatus.PENDING),
            BoostStep(7, "Ready to Launch ${targetGame.displayName}", "Verify summary before opening game...", BoostStepStatus.PENDING)
        )

        _uiState.value = _uiState.value.copy(
            showBoostDialog = true,
            isBoostInProgress = true,
            boostSteps = initialSteps,
            boostSummary = null
        )

        boostJob?.cancel()
        boostJob = viewModelScope.launch {
            val scoreBefore = _uiState.value.metrics.performanceScore
            val steps = initialSteps.toMutableList()
            val appliedSettings = mutableListOf<String>()
            val recommendations = mutableListOf<String>()

            // Step 1: Device performance
            delay(320L)
            val snap1 = repository.collectSnapshot(
                accessibleCacheBytes = _uiState.value.cacheScan.totalReclaimableBytes,
                lastPingMs = _uiState.value.metrics.lastPingMs,
                pingJitterMs = _uiState.value.metrics.pingJitterMs,
                packetLossPercent = _uiState.value.metrics.packetLossPercent,
                isSustainedActive = _uiState.value.sustainedPerformanceEnabled
            )
            steps[0] = steps[0].copy(
                detail = "Score ${snap1.performanceScore}/100 • ${snap1.displayRefreshRateHz.toInt()} Hz display • CPU load ${snap1.cpuUsagePercent}%",
                status = BoostStepStatus.COMPLETED
            )
            steps[1] = steps[1].copy(status = BoostStepStatus.RUNNING)
            _uiState.value = _uiState.value.copy(boostSteps = steps.toList(), metrics = snap1)

            // Step 2: Check Available RAM & trim optimizer buffers
            delay(320L)
            System.gc()
            val snap2 = repository.collectSnapshot(
                accessibleCacheBytes = _uiState.value.cacheScan.totalReclaimableBytes,
                lastPingMs = snap1.lastPingMs,
                pingJitterMs = snap1.pingJitterMs,
                packetLossPercent = snap1.packetLossPercent,
                isSustainedActive = _uiState.value.sustainedPerformanceEnabled
            )
            val ramStatus = if (snap2.isLowMemory || snap2.ramAvailableMb < 750L) {
                recommendations.add("Available RAM is tight (${snap2.ramAvailableMb} MB). Close recent heavy social/video apps from Recents before matchmaking.")
                BoostStepStatus.WARNING
            } else {
                BoostStepStatus.COMPLETED
            }
            steps[1] = steps[1].copy(
                detail = "${snap2.ramAvailableMb} MB free of ${snap2.ramTotalMb} MB (${100 - snap2.ramUsedPercent}% available)",
                status = ramStatus
            )
            steps[2] = steps[2].copy(status = BoostStepStatus.RUNNING)
            _uiState.value = _uiState.value.copy(boostSteps = steps.toList(), metrics = snap2)

            // Step 3: Check Battery Temperature & Thermal Status
            delay(300L)
            val tempDisplay = com.example.util.Formatters.formatTemperature(
                snap2.batteryTempCelsius,
                _uiState.value.useFahrenheit
            )
            val thermalWarning = snap2.thermalLevel == ThermalLevel.OVERHEATING ||
                snap2.thermalLevel == ThermalLevel.CRITICAL ||
                snap2.batteryTempCelsius >= _uiState.value.thermalThresholdCelsius
            if (thermalWarning) {
                recommendations.add("Device is warm ($tempDisplay). Use Smooth graphics, lower brightness to ~60%, and unplug charger while playing.")
            }
            if (snap2.batteryIsCharging) {
                recommendations.add("Charger connected (${snap2.batteryPluggedLabel}). Playing while charging generates extra heat and may trigger thermal throttling.")
            }
            steps[2] = steps[2].copy(
                detail = "$tempDisplay • Thermal: ${snap2.thermalStatusLabel} (${snap2.batteryLevel}%)",
                status = if (thermalWarning) BoostStepStatus.WARNING else BoostStepStatus.COMPLETED
            )
            steps[3] = steps[3].copy(status = BoostStepStatus.RUNNING)
            _uiState.value = _uiState.value.copy(boostSteps = steps.toList())

            // Step 4: Analyze & safely clean accessible temp files
            delay(320L)
            val scanBefore = repository.scanCache()
            var reclaimedBytes = 0L
            if (activeProfile?.autoCleanCache != false && scanBefore.totalReclaimableBytes > 0L) {
                reclaimedBytes = repository.cleanCache()
                appliedSettings.add("Cleared ${com.example.util.Formatters.formatBytes(reclaimedBytes)} accessible cache/temp files")
            } else if (scanBefore.totalReclaimableBytes > 0L) {
                recommendations.add("${com.example.util.Formatters.formatBytes(scanBefore.totalReclaimableBytes)} of accessible cache can be cleaned in Cache Cleaner.")
            }
            val scanAfter = repository.scanCache()
            steps[3] = steps[3].copy(
                detail = if (reclaimedBytes > 0L) {
                    "Reclaimed ${com.example.util.Formatters.formatBytes(reclaimedBytes)} • Game data & login untouched"
                } else {
                    "Accessible cache clean (${com.example.util.Formatters.formatBytes(scanBefore.totalReclaimableBytes)} scanned)"
                },
                status = BoostStepStatus.COMPLETED
            )
            steps[4] = steps[4].copy(status = BoostStepStatus.RUNNING)
            _uiState.value = _uiState.value.copy(
                boostSteps = steps.toList(),
                cacheScan = scanAfter.copy(lastCleanedBytes = reclaimedBytes)
            )

            // Step 5: Network pre-check & Safe Optimization Recommendations
            delay(320L)
            var pingResult = snap2.lastPingMs
            if (activeProfile?.runNetworkPreCheck != false && _uiState.value.selectedEndpoint != null) {
                pingResult = repository.pingOnce(_uiState.value.selectedEndpoint!!)
            }
            if (snap2.isPowerSaveMode) {
                recommendations.add("Android Battery Saver is ON — disable Battery Saver in Quick Settings so CPU/GPU clocks are not capped.")
            }
            if (!snap2.isOnline) {
                recommendations.add("No active internet connection detected. Check Wi-Fi or mobile data before opening Free Fire.")
            } else if (pingResult != null && pingResult > 100) {
                recommendations.add("Elevated latency (${pingResult} ms). Switch to 5 GHz Wi-Fi or move closer to your router.")
            }
            if (recommendations.isEmpty()) {
                recommendations.add("Suggested Free Fire preset (${activeProfile?.name ?: "Default"}): ${activeProfile?.recommendedGraphicsPreset ?: "Smooth + High FPS"}.")
            }
            steps[4] = steps[4].copy(
                detail = "${recommendations.size} device-aware insight(s) prepared" +
                    (pingResult?.let { " • Ping ${it}ms" } ?: ""),
                status = BoostStepStatus.COMPLETED
            )
            steps[5] = steps[5].copy(status = BoostStepStatus.RUNNING)
            _uiState.value = _uiState.value.copy(boostSteps = steps.toList())

            // Step 6: Apply Supported Performance Settings
            delay(320L)
            val wantSustained = activeProfile?.enableSustainedPerformance ?: _uiState.value.sustainedPerformanceEnabled
            if (snap2.isSustainedPerformanceSupported && wantSustained) {
                sustainedModeCallback?.invoke(true)
                appliedSettings.add("Android Sustained Performance hint active")
            }
            val wantDnd = activeProfile?.enableGamingDnd ?: _uiState.value.gamingDndEnabled
            if (wantDnd && snap2.isDndAccessGranted) {
                applyDndPriorityMode(true)
                appliedSettings.add("Gaming Do Not Disturb (Priority) enabled")
            } else if (wantDnd && !snap2.isDndAccessGranted) {
                recommendations.add("Grant Do Not Disturb permission in Game Settings to silence mid-match notifications automatically.")
            }
            if (appliedSettings.isEmpty()) {
                appliedSettings.add("Telemetry calibrated & safe runtime buffer trim completed")
            }
            steps[5] = steps[5].copy(
                detail = appliedSettings.joinToString(" • "),
                status = BoostStepStatus.COMPLETED
            )

            // Final telemetry check
            val finalSnap = repository.collectSnapshot(
                accessibleCacheBytes = scanAfter.totalReclaimableBytes,
                lastPingMs = pingResult,
                pingJitterMs = snap2.pingJitterMs,
                packetLossPercent = snap2.packetLossPercent,
                isSustainedActive = wantSustained && snap2.isSustainedPerformanceSupported
            )

            steps[6] = steps[6].copy(
                detail = if (targetGame.isInstalled) {
                    "${targetGame.displayName} verified and ready to launch"
                } else {
                    "${targetGame.displayName} is not installed on this device"
                },
                status = if (targetGame.isInstalled) BoostStepStatus.COMPLETED else BoostStepStatus.WARNING
            )

            val summary = OptimizationSummary(
                targetGame = targetGame,
                profileName = activeProfile?.name ?: "Competitive Ranked",
                scoreBefore = scoreBefore,
                scoreAfter = finalSnap.performanceScore,
                reclaimedCacheBytes = reclaimedBytes,
                availableRamMb = finalSnap.ramAvailableMb,
                batteryTempC = finalSnap.batteryTempCelsius,
                pingMs = pingResult,
                appliedSettings = appliedSettings,
                recommendations = recommendations
            )

            repository.recordOptimization(
                OptimizationHistoryEntity(
                    gameName = targetGame.displayName,
                    gamePackage = targetGame.packageName,
                    profileName = summary.profileName,
                    scoreBefore = summary.scoreBefore,
                    scoreAfter = summary.scoreAfter,
                    ramAvailableMb = summary.availableRamMb,
                    batteryTempCelsius = summary.batteryTempC,
                    pingMs = summary.pingMs,
                    cacheClearedBytes = summary.reclaimedCacheBytes,
                    actionsSummary = appliedSettings.joinToString("; ")
                )
            )

            _uiState.value = _uiState.value.copy(
                metrics = finalSnap,
                isBoostInProgress = false,
                boostSteps = steps.toList(),
                boostSummary = summary
            )
        }
    }

    fun dismissBoostDialog() {
        boostJob?.cancel()
        _uiState.value = _uiState.value.copy(
            showBoostDialog = false,
            isBoostInProgress = false
        )
    }

    fun launchTargetGameFromBoost(): Boolean {
        val target = _uiState.value.selectedGame ?: return false
        startSessionTimer()
        val launched = repository.launchGame(target.packageName)
        if (!launched) {
            _uiState.value = _uiState.value.copy(
                statusSnackbarMessage = "${target.displayName} is not installed. Select an installed game or install from Play Store."
            )
        } else {
            _uiState.value = _uiState.value.copy(showBoostDialog = false)
        }
        return launched
    }

    fun openPlayStoreForTarget() {
        val target = _uiState.value.selectedGame ?: return
        repository.gameDiscovery.openPlayStoreForGame(target.packageName)
    }

    fun startSessionTimer() {
        if (repository.prefs.sessionStartTimestampMs == 0L) {
            repository.prefs.sessionStartTimestampMs = System.currentTimeMillis()
        }
        _uiState.value = _uiState.value.copy(sessionRunning = true)
    }

    fun stopAndResetSessionTimer() {
        repository.prefs.sessionStartTimestampMs = 0L
        _uiState.value = _uiState.value.copy(
            sessionRunning = false,
            sessionElapsedMs = 0L
        )
    }

    fun selectProfile(profile: OptimizationProfileEntity) {
        repository.prefs.activeProfileId = profile.id
        repository.prefs.thermalThresholdCelsius = profile.thermalAlertThresholdC
        repository.prefs.gamingModeSustainedEnabled = profile.enableSustainedPerformance
        repository.prefs.gamingModeDndEnabled = profile.enableGamingDnd
        sustainedModeCallback?.invoke(profile.enableSustainedPerformance)
        _uiState.value = _uiState.value.copy(
            activeProfileId = profile.id,
            thermalThresholdCelsius = profile.thermalAlertThresholdC,
            sustainedPerformanceEnabled = profile.enableSustainedPerformance,
            gamingDndEnabled = profile.enableGamingDnd,
            statusSnackbarMessage = "Activated profile: ${profile.name}"
        )
    }

    fun createCustomProfile(
        name: String,
        description: String,
        autoCleanCache: Boolean,
        sustainedPerf: Boolean,
        gamingDnd: Boolean,
        networkCheck: Boolean,
        thermalThresholdC: Float,
        graphicsPreset: String
    ) {
        viewModelScope.launch {
            val id = repository.saveProfile(
                OptimizationProfileEntity(
                    name = name.trim(),
                    description = description.trim().ifEmpty { "Custom gaming profile" },
                    isBuiltIn = false,
                    autoCleanCache = autoCleanCache,
                    enableSustainedPerformance = sustainedPerf,
                    enableGamingDnd = gamingDnd,
                    runNetworkPreCheck = networkCheck,
                    thermalAlertThresholdC = thermalThresholdC,
                    recommendedGraphicsPreset = graphicsPreset
                )
            )
            repository.prefs.activeProfileId = id
            _uiState.value = _uiState.value.copy(
                activeProfileId = id,
                statusSnackbarMessage = "Saved custom profile: $name"
            )
        }
    }

    fun deleteCustomProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomProfile(id)
            if (_uiState.value.activeProfileId == id) {
                repository.prefs.activeProfileId = 1L
                _uiState.value = _uiState.value.copy(activeProfileId = 1L)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.value = _uiState.value.copy(statusSnackbarMessage = "Optimization history cleared")
        }
    }

    fun setUseFahrenheit(useF: Boolean) {
        repository.prefs.useFahrenheit = useF
        _uiState.value = _uiState.value.copy(useFahrenheit = useF)
    }

    fun setOverheatingAlertsEnabled(enabled: Boolean) {
        repository.prefs.overheatingAlertsEnabled = enabled
        _uiState.value = _uiState.value.copy(overheatingAlertsEnabled = enabled)
    }

    fun setThermalThreshold(tempC: Float) {
        repository.prefs.thermalThresholdCelsius = tempC
        _uiState.value = _uiState.value.copy(thermalThresholdCelsius = tempC)
    }

    fun setSustainedPerformanceEnabled(enabled: Boolean) {
        repository.prefs.gamingModeSustainedEnabled = enabled
        sustainedModeCallback?.invoke(enabled)
        _uiState.value = _uiState.value.copy(sustainedPerformanceEnabled = enabled)
        refreshTelemetryOnce(measurePing = false)
    }

    fun setGamingDndEnabled(enabled: Boolean): Boolean {
        val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            return false
        }
        applyDndPriorityMode(enabled)
        repository.prefs.gamingModeDndEnabled = enabled
        _uiState.value = _uiState.value.copy(gamingDndEnabled = enabled)
        refreshTelemetryOnce(measurePing = false)
        return true
    }

    private fun applyDndPriorityMode(enable: Boolean) {
        val nm = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(
                if (enable) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(statusSnackbarMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        boostJob?.cancel()
        stopFpsMonitoring()
    }
}
