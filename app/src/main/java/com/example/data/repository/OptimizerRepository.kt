package com.example.data.repository

import android.content.Context
import com.example.data.local.OptimizationHistoryEntity
import com.example.data.local.OptimizationProfileEntity
import com.example.data.local.OptimizerDao
import com.example.data.local.OptimizerDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.BackgroundAppEntry
import com.example.data.model.CacheScanResult
import com.example.data.model.DeviceHardwareSpec
import com.example.data.model.DeviceMetricsSnapshot
import com.example.data.model.GameEntry
import com.example.data.model.PingEndpoint
import com.example.data.model.PingTestReport
import com.example.data.system.BackgroundActivityManager
import com.example.data.system.CacheCleanerManager
import com.example.data.system.DeviceTelemetryMonitor
import com.example.data.system.GameDiscoveryManager
import com.example.data.system.NetworkDiagnosticsManager
import com.example.data.system.ThermalAlertNotifier
import kotlinx.coroutines.flow.Flow

class OptimizerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao: OptimizerDao = OptimizerDatabase.getInstance(appContext).optimizerDao()
    val prefs = PreferencesManager(appContext)

    val telemetryMonitor = DeviceTelemetryMonitor(appContext)
    val gameDiscovery = GameDiscoveryManager(appContext)
    val cacheCleaner = CacheCleanerManager(appContext)
    val networkDiagnostics = NetworkDiagnosticsManager(appContext)
    val backgroundActivity = BackgroundActivityManager(appContext)
    val thermalNotifier = ThermalAlertNotifier(appContext)

    val historyFlow: Flow<List<OptimizationHistoryEntity>> = dao.observeHistory()
    val profilesFlow: Flow<List<OptimizationProfileEntity>> = dao.observeProfiles()

    suspend fun ensureDefaultProfilesSeeded() {
        if (dao.getProfileCount() == 0) {
            dao.insertProfiles(
                listOf(
                    OptimizationProfileEntity(
                        id = 1L,
                        name = "Competitive Ranked",
                        description = "Prioritizes clear cache, low-latency ping check, Gaming DND, and sustained clocks for Clash Squad & BR Ranked.",
                        isBuiltIn = true,
                        autoCleanCache = true,
                        enableSustainedPerformance = true,
                        enableGamingDnd = true,
                        runNetworkPreCheck = true,
                        thermalAlertThresholdC = 40.5f,
                        recommendedGraphicsPreset = "Smooth + High FPS"
                    ),
                    OptimizationProfileEntity(
                        id = 2L,
                        name = "Balanced Survival",
                        description = "Balanced thermal and battery profile for longer classic sessions without aggressive heat buildup.",
                        isBuiltIn = true,
                        autoCleanCache = true,
                        enableSustainedPerformance = true,
                        enableGamingDnd = false,
                        runNetworkPreCheck = true,
                        thermalAlertThresholdC = 39.0f,
                        recommendedGraphicsPreset = "Standard + Normal FPS"
                    ),
                    OptimizationProfileEntity(
                        id = 3L,
                        name = "Thermal Cool-Down",
                        description = "Strict thermal protection with lower alert limit and cooling guidance for warm environments.",
                        isBuiltIn = true,
                        autoCleanCache = false,
                        enableSustainedPerformance = false,
                        enableGamingDnd = false,
                        runNetworkPreCheck = false,
                        thermalAlertThresholdC = 37.5f,
                        recommendedGraphicsPreset = "Smooth + Standard FPS + Shadows Off"
                    )
                )
            )
        }
    }

    suspend fun recordOptimization(entry: OptimizationHistoryEntity) {
        dao.insertHistory(entry)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun saveProfile(profile: OptimizationProfileEntity): Long {
        return dao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: OptimizationProfileEntity) {
        dao.updateProfile(profile)
    }

    suspend fun deleteCustomProfile(id: Long) {
        dao.deleteCustomProfile(id)
    }

    fun collectSnapshot(
        accessibleCacheBytes: Long,
        lastPingMs: Int?,
        pingJitterMs: Int?,
        packetLossPercent: Int?,
        isSustainedActive: Boolean
    ): DeviceMetricsSnapshot {
        val net = networkDiagnostics.readNetworkTelemetry()
        return telemetryMonitor.collectSnapshot(
            accessibleCacheBytes = accessibleCacheBytes,
            networkTelemetry = net,
            lastPingMs = lastPingMs,
            pingJitterMs = pingJitterMs,
            packetLossPercent = packetLossPercent,
            isSustainedActive = isSustainedActive
        )
    }

    fun getHardwareSpecs(): DeviceHardwareSpec = telemetryMonitor.getHardwareSpecs()

    suspend fun scanCache(): CacheScanResult =
        cacheCleaner.scanAccessibleCache(prefs.safCacheTreeUri)

    suspend fun cleanCache(): Long =
        cacheCleaner.cleanAccessibleCache(prefs.safCacheTreeUri)

    suspend fun getBackgroundApps(targetPkg: String, hasUsageAccess: Boolean): List<BackgroundAppEntry> =
        backgroundActivity.getBackgroundActivity(targetPkg, hasUsageAccess)

    fun getPrimaryTargets(): List<GameEntry> = gameDiscovery.getPrimaryFreeFireTargets()

    fun getDetectedGames(): List<GameEntry> =
        gameDiscovery.detectInstalledGames(prefs.manualCustomPackages)

    fun getAllLaunchableApps(): List<GameEntry> = gameDiscovery.getAllLaunchableAppsForManualPicker()

    fun resolveTargetGame(pkg: String): GameEntry = gameDiscovery.resolveTargetGame(pkg)

    fun launchGame(pkg: String): Boolean = gameDiscovery.launchGame(pkg)

    fun getEndpoints(): List<PingEndpoint> = networkDiagnostics.availableEndpoints

    suspend fun pingOnce(endpoint: PingEndpoint): Int? =
        networkDiagnostics.measureSinglePing(endpoint)

    suspend fun runStabilityTest(
        endpoint: PingEndpoint,
        onProgressSample: (List<Int?>) -> Unit
    ): PingTestReport = networkDiagnostics.runStabilityTest(endpoint, 8, onProgressSample)
}
