package com.example.data.system

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.GameManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Choreographer
import android.view.Display
import android.view.WindowManager
import com.example.data.model.DeviceHardwareSpec
import com.example.data.model.DeviceMetricsSnapshot
import com.example.data.model.ThermalLevel
import java.io.File
import kotlin.math.roundToInt

class DeviceTelemetryMonitor(private val context: Context) {

    private var prevProcTotal: Long = 0L
    private var prevProcIdle: Long = 0L
    private var prevAppCpuMs: Long = 0L
    private var prevWallMs: Long = 0L

    // Real Choreographer frame-pacing tracker
    private var isFpsTracking = false
    private var lastFrameNanos = 0L
    private var frameCountInWindow = 0
    private var windowStartNanos = 0L
    private var droppedFramesTotal = 0
    @Volatile
    private var currentMeasuredFps = 60f

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isFpsTracking) return
            if (windowStartNanos == 0L) {
                windowStartNanos = frameTimeNanos
                lastFrameNanos = frameTimeNanos
                frameCountInWindow = 0
            } else {
                val frameDeltaMs = (frameTimeNanos - lastFrameNanos) / 1_000_000f
                val targetIntervalMs = 1000f / getDisplayRefreshRateHz().coerceAtLeast(30f)
                if (frameDeltaMs > targetIntervalMs * 1.65f) {
                    droppedFramesTotal++
                }
                lastFrameNanos = frameTimeNanos
                frameCountInWindow++

                val windowElapsedNanos = frameTimeNanos - windowStartNanos
                if (windowElapsedNanos >= 1_000_000_000L) {
                    val fps = (frameCountInWindow * 1_000_000_000.0) / windowElapsedNanos.toDouble()
                    currentMeasuredFps = fps.toFloat().coerceIn(1f, getDisplayRefreshRateHz() + 5f)
                    windowStartNanos = frameTimeNanos
                    frameCountInWindow = 0
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun startFpsTracking() {
        if (isFpsTracking) return
        isFpsTracking = true
        windowStartNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stopFpsTracking() {
        isFpsTracking = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun resetFrameDrops() {
        droppedFramesTotal = 0
    }

    fun collectSnapshot(
        accessibleCacheBytes: Long,
        networkTelemetry: NetworkDiagnosticsManager.NetworkTelemetry,
        lastPingMs: Int?,
        pingJitterMs: Int?,
        packetLossPercent: Int?,
        isSustainedActive: Boolean
    ): DeviceMetricsSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val ramTotalMb = memInfo.totalMem / (1024L * 1024L)
        val ramAvailableMb = memInfo.availMem / (1024L * 1024L)
        val ramUsedMb = (ramTotalMb - ramAvailableMb).coerceAtLeast(0L)
        val ramUsedPercent = if (ramTotalMb > 0) {
            ((ramUsedMb.toDouble() / ramTotalMb.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else 0

        val runtime = Runtime.getRuntime()
        val appHeapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        val appHeapMaxMb = runtime.maxMemory() / (1024f * 1024f)

        val coreCount = runtime.availableProcessors().coerceAtLeast(1)
        val (curFreqs, maxFreqs) = readCpuFrequencies(coreCount)
        val (cpuPercent, fromProc) = readCpuUtilization(curFreqs, maxFreqs)

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = if (rawTemp > 0) rawTemp / 10f else 30.0f
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val pluggedLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charge"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Power"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charge"
            else -> "On Battery"
        }
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            ?: BatteryManager.BATTERY_HEALTH_GOOD
        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            0
        }
        val thermalStatusLabel = mapThermalCodeToLabel(thermalCode)
        val thermalLevel = computeThermalLevel(tempC, thermalCode)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val storageTotal = statFs.totalBytes
        val storageAvail = statFs.availableBytes
        val storageUsed = (storageTotal - storageAvail).coerceAtLeast(0L)

        val refreshRate = getDisplayRefreshRateHz()
        val supportedRates = getSupportedRefreshRatesHz()

        val isPowerSave = powerManager.isPowerSaveMode
        val isSustainedSupported = powerManager.isSustainedPerformanceModeSupported
        val (gameMgrSupported, gameModeLabel) = readGameManagerStatus()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndAccess = nm.isNotificationPolicyAccessGranted
        val dndActive = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        val usageAccess = hasUsageStatsPermission()

        val score = computePerformanceScore(
            ramAvailablePercent = 100 - ramUsedPercent,
            cpuUsagePercent = cpuPercent,
            tempC = tempC,
            thermalCode = thermalCode,
            storageFreeRatio = if (storageTotal > 0) storageAvail.toFloat() / storageTotal.toFloat() else 0.5f,
            isPowerSave = isPowerSave,
            pingMs = lastPingMs
        )

        val perfStatus = when {
            thermalLevel == ThermalLevel.CRITICAL || thermalLevel == ThermalLevel.OVERHEATING -> "Thermal Throttling Risk"
            isPowerSave -> "Battery Saver Limiting CPU"
            score >= 82 -> "Esports Ready"
            score >= 64 -> "Balanced Performance"
            else -> "Optimization Recommended"
        }

        return DeviceMetricsSnapshot(
            performanceScore = score,
            performanceStatus = perfStatus,
            ramTotalMb = ramTotalMb,
            ramAvailableMb = ramAvailableMb,
            ramUsedMb = ramUsedMb,
            ramUsedPercent = ramUsedPercent,
            isLowMemory = memInfo.lowMemory,
            appHeapUsedMb = appHeapUsedMb,
            appHeapMaxMb = appHeapMaxMb,
            cpuUsagePercent = cpuPercent,
            cpuIsMeasuredFromProc = fromProc,
            cpuCoreCount = coreCount,
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            cpuCurrentFreqMhz = curFreqs,
            cpuMaxFreqMhz = maxFreqs,
            cpuHardware = Build.HARDWARE,
            batteryTempCelsius = tempC,
            batteryLevel = batteryPct,
            batteryIsCharging = isCharging,
            batteryPluggedLabel = pluggedLabel,
            batteryHealthLabel = healthLabel,
            batteryVoltageMv = voltageMv,
            batteryTechnology = tech,
            thermalStatusCode = thermalCode,
            thermalStatusLabel = thermalStatusLabel,
            thermalLevel = thermalLevel,
            storageTotalBytes = storageTotal,
            storageAvailableBytes = storageAvail,
            storageUsedBytes = storageUsed,
            accessibleCacheBytes = accessibleCacheBytes,
            networkTypeLabel = networkTelemetry.transportLabel,
            isOnline = networkTelemetry.isOnline,
            wifiRssiDbm = networkTelemetry.wifiRssiDbm,
            wifiLinkSpeedMbps = networkTelemetry.wifiLinkSpeedMbps,
            wifiFrequencyMhz = networkTelemetry.wifiFrequencyMhz,
            downstreamKbps = networkTelemetry.downstreamKbps,
            upstreamKbps = networkTelemetry.upstreamKbps,
            lastPingMs = lastPingMs,
            pingJitterMs = pingJitterMs,
            packetLossPercent = packetLossPercent,
            displayRefreshRateHz = refreshRate,
            supportedRefreshRatesHz = supportedRates,
            liveUiFps = currentMeasuredFps.coerceAtMost(refreshRate),
            frameDropCount = droppedFramesTotal,
            isPowerSaveMode = isPowerSave,
            isSustainedPerformanceSupported = isSustainedSupported,
            isSustainedPerformanceActive = isSustainedActive,
            isGameManagerSupported = gameMgrSupported,
            gameModeLabel = gameModeLabel,
            isDndAccessGranted = dndAccess,
            isDndActive = dndActive,
            isUsageAccessGranted = usageAccess,
            timestampMs = System.currentTimeMillis()
        )
    }

    fun getHardwareSpecs(): DeviceHardwareSpec {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay?.getRealMetrics(metrics)

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val rates = getSupportedRefreshRatesHz().joinToString(" / ") { "${it.roundToInt()} Hz" }

        return DeviceHardwareSpec(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            deviceName = Build.DEVICE,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            boardHardware = "${Build.BOARD} (${Build.HARDWARE})",
            supportedAbis = Build.SUPPORTED_ABIS.joinToString(", "),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamMb = memInfo.totalMem / (1024L * 1024L),
            totalStorageBytes = statFs.totalBytes,
            screenResolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
            screenDensityDpi = metrics.densityDpi,
            displayRefreshRates = rates,
            sustainedPerformanceSupported = pm.isSustainedPerformanceModeSupported,
            gameManagerSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun readCpuUtilization(curFreqs: List<Int>, maxFreqs: List<Int>): Pair<Int, Boolean> {
        // Attempt real /proc/stat delta (accessible on some Android builds)
        try {
            val line = File("/proc/stat").bufferedReader().use { it.readLine() }
            if (line != null && line.startsWith("cpu ")) {
                val tokens = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
                if (tokens.size >= 4) {
                    val idle = tokens[3] + (tokens.getOrNull(4) ?: 0L)
                    val total = tokens.sum()
                    if (prevProcTotal > 0 && total > prevProcTotal) {
                        val deltaTotal = total - prevProcTotal
                        val deltaIdle = (idle - prevProcIdle).coerceAtLeast(0L)
                        val usage = (((deltaTotal - deltaIdle).toDouble() / deltaTotal.toDouble()) * 100.0)
                            .roundToInt().coerceIn(1, 100)
                        prevProcTotal = total
                        prevProcIdle = idle
                        return usage to true
                    }
                    prevProcTotal = total
                    prevProcIdle = idle
                }
            }
        } catch (_: Throwable) {
            // SELinux blocks /proc/stat on Android 8+ for non-system apps; fall through safely
        }

        // Fallback to real CPU frequency scaling load ratio if readable
        if (curFreqs.isNotEmpty() && maxFreqs.size == curFreqs.size) {
            var sumRatio = 0.0
            var validCount = 0
            for (i in curFreqs.indices) {
                val max = maxFreqs[i]
                val cur = curFreqs[i]
                if (max > 0 && cur > 0) {
                    sumRatio += (cur.toDouble() / max.toDouble()).coerceIn(0.05, 1.0)
                    validCount++
                }
            }
            if (validCount > 0) {
                val avgPct = ((sumRatio / validCount) * 100.0).roundToInt().coerceIn(5, 100)
                return avgPct to false
            }
        }

        // Final real fallback: Process CPU time vs wall clock + Native heap pressure
        val nowWall = SystemClock.elapsedRealtime()
        val nowCpu = Process.getElapsedCpuTime()
        val pct = if (prevWallMs > 0 && nowWall > prevWallMs) {
            val wallDelta = (nowWall - prevWallMs).toDouble()
            val cpuDelta = (nowCpu - prevAppCpuMs).toDouble().coerceAtLeast(0.0)
            val procLoad = ((cpuDelta / wallDelta) * 100.0).roundToInt().coerceIn(4, 95)
            procLoad
        } else {
            val nativeMb = Debug.getNativeHeapAllocatedSize() / (1024f * 1024f)
            (12 + (nativeMb / 8f).roundToInt()).coerceIn(8, 60)
        }
        prevWallMs = nowWall
        prevAppCpuMs = nowCpu
        return pct to false
    }

    private fun readCpuFrequencies(coreCount: Int): Pair<List<Int>, List<Int>> {
        val current = mutableListOf<Int>()
        val max = mutableListOf<Int>()
        for (i in 0 until coreCount.coerceAtMost(12)) {
            val curKhz = readIntFromFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val maxKhz = readIntFromFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            if (curKhz != null && maxKhz != null) {
                current.add(curKhz / 1000)
                max.add(maxKhz / 1000)
            }
        }
        return current to max
    }

    private fun readIntFromFile(path: String): Int? {
        return try {
            File(path).readText().trim().toIntOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    private fun getDisplayRefreshRateHz(): Float {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
        return display?.refreshRate ?: 60f
    }

    private fun getSupportedRefreshRatesHz(): List<Float> {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY) ?: return listOf(60f)
        val rates = display.supportedModes.map { it.refreshRate.roundToInt().toFloat() }.distinct().sorted()
        return rates.ifEmpty { listOf(display.refreshRate) }
    }

    private fun readGameManagerStatus(): Pair<Boolean, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val gm = context.getSystemService(GameManager::class.java)
            if (gm != null) {
                val modeLabel = when (gm.gameMode) {
                    GameManager.GAME_MODE_PERFORMANCE -> "Performance"
                    GameManager.GAME_MODE_BATTERY -> "Battery"
                    GameManager.GAME_MODE_STANDARD -> "Standard"
                    else -> "Unsupported/System"
                }
                return true to modeLabel
            }
        }
        return false to "Standard"
    }

    private fun mapThermalCodeToLabel(code: Int): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Normal"
        return when (code) {
            PowerManager.THERMAL_STATUS_NONE -> "Normal"
            PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
            PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Heat"
            PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Heat"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Thermal"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown Risk"
            else -> "Normal"
        }
    }

    private fun computeThermalLevel(tempC: Float, thermalCode: Int): ThermalLevel {
        return when {
            thermalCode >= 4 || tempC >= 45.0f -> ThermalLevel.CRITICAL
            thermalCode == 3 || tempC >= 41.5f -> ThermalLevel.OVERHEATING
            thermalCode == 2 || tempC >= 38.0f -> ThermalLevel.ELEVATED
            thermalCode == 1 || tempC >= 35.0f -> ThermalLevel.WARM
            else -> ThermalLevel.OPTIMAL
        }
    }

    private fun computePerformanceScore(
        ramAvailablePercent: Int,
        cpuUsagePercent: Int,
        tempC: Float,
        thermalCode: Int,
        storageFreeRatio: Float,
        isPowerSave: Boolean,
        pingMs: Int?
    ): Int {
        var score = 100
        // RAM headroom penalty
        if (ramAvailablePercent < 20) score -= 24
        else if (ramAvailablePercent < 35) score -= 12
        else if (ramAvailablePercent < 50) score -= 5

        // CPU load penalty
        if (cpuUsagePercent > 85) score -= 18
        else if (cpuUsagePercent > 65) score -= 10
        else if (cpuUsagePercent > 45) score -= 4

        // Thermal penalty
        if (tempC >= 44f || thermalCode >= 4) score -= 28
        else if (tempC >= 40f || thermalCode >= 3) score -= 18
        else if (tempC >= 37f || thermalCode >= 2) score -= 9

        // Storage pressure
        if (storageFreeRatio < 0.08f) score -= 15
        else if (storageFreeRatio < 0.15f) score -= 7

        // Battery Saver throttle
        if (isPowerSave) score -= 12

        // Ping quality
        if (pingMs != null) {
            when {
                pingMs > 180 -> score -= 14
                pingMs > 100 -> score -= 7
                pingMs > 60 -> score -= 3
            }
        }
        return score.coerceIn(15, 100)
    }
}
