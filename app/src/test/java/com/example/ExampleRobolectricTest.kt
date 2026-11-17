package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.system.CacheCleanerManager
import com.example.data.system.DeviceTelemetryMonitor
import com.example.data.system.GameDiscoveryManager
import com.example.data.system.NetworkDiagnosticsManager
import com.example.util.Formatters
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches FF Pro Optimizer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FF Pro Optimizer", appName)
    }

    @Test
    fun `cache cleaner scans and removes temp files without touching account data`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File(context.cacheDir, "shader_temp.tmp").apply {
            writeText("temporary shader buffer 12345")
        }
        val protectedFile = File(context.filesDir, "user_account_login.dat").apply {
            writeText("important_session_token")
        }

        val cleaner = CacheCleanerManager(context)
        val scanResult = cleaner.scanAccessibleCache(null)
        assertTrue(scanResult.totalReclaimableBytes >= tempFile.length())

        val freedBytes = cleaner.cleanAccessibleCache(null)
        assertTrue(freedBytes > 0L)
        assertFalse(tempFile.exists())
        assertTrue(protectedFile.exists())
        protectedFile.delete()
    }

    @Test
    fun `game discovery resolves Free Fire and Free Fire MAX targets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val discovery = GameDiscoveryManager(context)
        val targets = discovery.getPrimaryFreeFireTargets()
        assertEquals(2, targets.size)
        assertEquals(GameDiscoveryManager.PKG_FREE_FIRE, targets[0].packageName)
        assertEquals(GameDiscoveryManager.PKG_FREE_FIRE_MAX, targets[1].packageName)
    }

    @Test
    fun `telemetry monitor collects valid device metrics snapshot`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val monitor = DeviceTelemetryMonitor(context)
        val net = NetworkDiagnosticsManager(context).readNetworkTelemetry()
        val snapshot = monitor.collectSnapshot(
            accessibleCacheBytes = 1024L,
            networkTelemetry = net,
            lastPingMs = 35,
            pingJitterMs = 4,
            packetLossPercent = 0,
            isSustainedActive = false
        )
        assertTrue(snapshot.performanceScore in 15..100)
        assertTrue(snapshot.cpuCoreCount >= 1)
        assertTrue(monitor.getHardwareSpecs().apiLevel >= 24)
    }

    @Test
    fun `formatters output readable units`() {
        assertEquals("1.5 MB", Formatters.formatBytes(1572864L))
        assertEquals("38.5°C", Formatters.formatTemperature(38.5f, false))
        assertEquals("01:05", Formatters.formatDurationHms(65_000L))
    }
}
