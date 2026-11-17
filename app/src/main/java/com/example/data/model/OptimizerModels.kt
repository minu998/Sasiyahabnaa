package com.example.data.model

import android.graphics.drawable.Drawable

enum class GameTargetType {
    FREE_FIRE,
    FREE_FIRE_MAX,
    DETECTED_GAME,
    MANUAL_CUSTOM
}

data class GameEntry(
    val packageName: String,
    val displayName: String,
    val isInstalled: Boolean,
    val versionName: String?,
    val targetType: GameTargetType,
    val appIcon: Drawable? = null
)

enum class ThermalLevel(val label: String) {
    OPTIMAL("Cool & Optimal"),
    WARM("Warm Load"),
    ELEVATED("Elevated Heat"),
    OVERHEATING("Overheating Warning"),
    CRITICAL("Critical Thermal")
}

data class DeviceMetricsSnapshot(
    val performanceScore: Int = 0,
    val performanceStatus: String = "Analyzing",
    // RAM
    val ramTotalMb: Long = 0L,
    val ramAvailableMb: Long = 0L,
    val ramUsedMb: Long = 0L,
    val ramUsedPercent: Int = 0,
    val isLowMemory: Boolean = false,
    val appHeapUsedMb: Float = 0f,
    val appHeapMaxMb: Float = 0f,
    // CPU
    val cpuUsagePercent: Int = 0,
    val cpuIsMeasuredFromProc: Boolean = false,
    val cpuCoreCount: Int = 1,
    val cpuAbi: String = "",
    val cpuCurrentFreqMhz: List<Int> = emptyList(),
    val cpuMaxFreqMhz: List<Int> = emptyList(),
    val cpuHardware: String = "",
    // Battery & Thermal
    val batteryTempCelsius: Float = 0f,
    val batteryLevel: Int = 0,
    val batteryIsCharging: Boolean = false,
    val batteryPluggedLabel: String = "On Battery",
    val batteryHealthLabel: String = "Good",
    val batteryVoltageMv: Int = 0,
    val batteryTechnology: String = "Li-ion",
    val thermalStatusCode: Int = 0,
    val thermalStatusLabel: String = "Normal",
    val thermalLevel: ThermalLevel = ThermalLevel.OPTIMAL,
    // Storage
    val storageTotalBytes: Long = 0L,
    val storageAvailableBytes: Long = 0L,
    val storageUsedBytes: Long = 0L,
    val accessibleCacheBytes: Long = 0L,
    // Network
    val networkTypeLabel: String = "Checking...",
    val isOnline: Boolean = false,
    val wifiRssiDbm: Int? = null,
    val wifiLinkSpeedMbps: Int? = null,
    val wifiFrequencyMhz: Int? = null,
    val downstreamKbps: Int = 0,
    val upstreamKbps: Int = 0,
    val lastPingMs: Int? = null,
    val pingJitterMs: Int? = null,
    val packetLossPercent: Int? = null,
    // Display & FPS
    val displayRefreshRateHz: Float = 60f,
    val supportedRefreshRatesHz: List<Float> = listOf(60f),
    val liveUiFps: Float = 60f,
    val frameDropCount: Int = 0,
    // System Modes & Permissions
    val isPowerSaveMode: Boolean = false,
    val isSustainedPerformanceSupported: Boolean = false,
    val isSustainedPerformanceActive: Boolean = false,
    val isGameManagerSupported: Boolean = false,
    val gameModeLabel: String = "Standard",
    val isDndAccessGranted: Boolean = false,
    val isDndActive: Boolean = false,
    val isUsageAccessGranted: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class CacheCategory(val label: String) {
    APP_CACHE("App Cache"),
    TEMP_FILES("Temporary Files"),
    CODE_CACHE("Shader / Code Cache"),
    EXTERNAL_CACHE("External Cache"),
    USER_SELECTED_FOLDER("User-Selected Folder")
}

data class CacheItemEntry(
    val id: String,
    val name: String,
    val pathDisplay: String,
    val sizeBytes: Long,
    val category: CacheCategory,
    val isSafeToClean: Boolean = true
)

data class CacheScanResult(
    val items: List<CacheItemEntry> = emptyList(),
    val totalReclaimableBytes: Long = 0L,
    val lastCleanedBytes: Long = 0L,
    val lastScanTimeMs: Long = 0L,
    val safFolderName: String? = null
)

data class BackgroundAppEntry(
    val packageName: String,
    val appName: String,
    val lastUsedMs: Long,
    val foregroundMinutes: Long,
    val importanceLabel: String,
    val isSystemCritical: Boolean,
    val appIcon: Drawable? = null
)

data class PingEndpoint(
    val id: String,
    val label: String,
    val host: String,
    val port: Int,
    val regionTag: String
)

data class PingTestReport(
    val endpoint: PingEndpoint,
    val samplesMs: List<Int?> = emptyList(),
    val avgMs: Int? = null,
    val minMs: Int? = null,
    val maxMs: Int? = null,
    val jitterMs: Int? = null,
    val packetLossPercent: Int = 0,
    val stabilityLabel: String = "Not Tested",
    val timestampMs: Long = 0L
)

enum class BoostStepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    WARNING
}

data class BoostStep(
    val stepNumber: Int,
    val title: String,
    val detail: String,
    val status: BoostStepStatus = BoostStepStatus.PENDING
)

data class OptimizationSummary(
    val targetGame: GameEntry,
    val profileName: String,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val reclaimedCacheBytes: Long,
    val availableRamMb: Long,
    val batteryTempC: Float,
    val pingMs: Int?,
    val appliedSettings: List<String>,
    val recommendations: List<String>,
    val timestampMs: Long = System.currentTimeMillis()
)

data class DeviceHardwareSpec(
    val manufacturer: String,
    val model: String,
    val deviceName: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val boardHardware: String,
    val supportedAbis: String,
    val cpuCores: Int,
    val totalRamMb: Long,
    val totalStorageBytes: Long,
    val screenResolution: String,
    val screenDensityDpi: Int,
    val displayRefreshRates: String,
    val sustainedPerformanceSupported: Boolean,
    val gameManagerSupported: Boolean
)
