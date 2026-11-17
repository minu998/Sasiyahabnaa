package com.example.data.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import com.example.data.model.PingEndpoint
import com.example.data.model.PingTestReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class NetworkDiagnosticsManager(private val context: Context) {

    data class NetworkTelemetry(
        val isOnline: Boolean,
        val transportLabel: String,
        val wifiRssiDbm: Int?,
        val wifiLinkSpeedMbps: Int?,
        val wifiFrequencyMhz: Int?,
        val downstreamKbps: Int,
        val upstreamKbps: Int
    )

    val availableEndpoints: List<PingEndpoint> = listOf(
        PingEndpoint("cloudflare", "Cloudflare Edge (1.1.1.1)", "1.1.1.1", 443, "Global Anycast"),
        PingEndpoint("google_dns", "Google DNS (8.8.8.8)", "8.8.8.8", 53, "Global Anycast"),
        PingEndpoint("quad9", "Quad9 Relay (9.9.9.9)", "9.9.9.9", 443, "Low-Jitter"),
        PingEndpoint("opendns", "OpenDNS (208.67.222.222)", "208.67.222.222", 53, "Backup Route")
    )

    fun readNetworkTelemetry(): NetworkTelemetry {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isOnline = caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (caps == null) {
            return NetworkTelemetry(
                isOnline = false,
                transportLabel = "Offline",
                wifiRssiDbm = null,
                wifiLinkSpeedMbps = null,
                wifiFrequencyMhz = null,
                downstreamKbps = 0,
                upstreamKbps = 0
            )
        }

        val downKbps = caps.linkDownstreamBandwidthKbps
        val upKbps = caps.linkUpstreamBandwidthKbps

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val wifiInfo = wm.connectionInfo
            val rssi = if (wifiInfo != null && wifiInfo.rssi != -127) wifiInfo.rssi else null
            val linkSpeed = if (wifiInfo != null && wifiInfo.linkSpeed > 0) wifiInfo.linkSpeed else null
            val freq = if (wifiInfo != null && wifiInfo.frequency > 0) wifiInfo.frequency else null
            val band = when {
                freq != null && freq >= 5925 -> "Wi-Fi 6E (6 GHz)"
                freq != null && freq >= 4900 -> "Wi-Fi 5 GHz"
                freq != null && freq >= 2400 -> "Wi-Fi 2.4 GHz"
                else -> "Wi-Fi"
            }
            return NetworkTelemetry(
                isOnline = isOnline || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                transportLabel = band,
                wifiRssiDbm = rssi,
                wifiLinkSpeedMbps = linkSpeed,
                wifiFrequencyMhz = freq,
                downstreamKbps = downKbps,
                upstreamKbps = upKbps
            )
        }

        val label = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && downKbps >= 100_000) "5G / High-Speed Mobile"
                else "Mobile Data (4G/5G)"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Tunnel"
            else -> "Connected"
        }

        return NetworkTelemetry(
            isOnline = isOnline || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            transportLabel = label,
            wifiRssiDbm = null,
            wifiLinkSpeedMbps = null,
            wifiFrequencyMhz = null,
            downstreamKbps = downKbps,
            upstreamKbps = upKbps
        )
    }

    suspend fun measureSinglePing(endpoint: PingEndpoint, timeoutMs: Int = 2000): Int? =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                val startNs = System.nanoTime()
                socket.connect(InetSocketAddress(endpoint.host, endpoint.port), timeoutMs)
                val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000.0).roundToInt()
                elapsedMs.coerceAtLeast(1)
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { socket.close() }
            }
        }

    suspend fun runStabilityTest(
        endpoint: PingEndpoint,
        sampleCount: Int = 8,
        onProgressSample: ((List<Int?>) -> Unit)? = null
    ): PingTestReport = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Int?>()
        repeat(sampleCount) {
            val rtt = measureSinglePing(endpoint, timeoutMs = 1800)
            samples.add(rtt)
            onProgressSample?.invoke(samples.toList())
            if (it < sampleCount - 1) {
                kotlinx.coroutines.delay(140L)
            }
        }

        val successful = samples.filterNotNull()
        val lossPct = (((sampleCount - successful.size).toDouble() / sampleCount.toDouble()) * 100.0).roundToInt()

        if (successful.isEmpty()) {
            return@withContext PingTestReport(
                endpoint = endpoint,
                samplesMs = samples,
                avgMs = null,
                minMs = null,
                maxMs = null,
                jitterMs = null,
                packetLossPercent = 100,
                stabilityLabel = "Unreachable / Offline",
                timestampMs = System.currentTimeMillis()
            )
        }

        val avg = successful.average().roundToInt()
        val min = successful.minOrNull()
        val max = successful.maxOrNull()

        // Jitter = mean consecutive RTT difference (RFC 3550 style) or std dev
        val jitter = if (successful.size >= 2) {
            var diffSum = 0.0
            for (i in 1 until successful.size) {
                diffSum += abs(successful[i] - successful[i - 1])
            }
            (diffSum / (successful.size - 1)).roundToInt()
        } else {
            val mean = successful.average()
            val variance = successful.map { (it - mean) * (it - mean) }.average()
            sqrt(variance).roundToInt()
        }

        val stability = when {
            lossPct >= 25 || avg > 180 -> "Unstable (High Lag Risk)"
            lossPct > 0 || jitter > 30 || avg > 100 -> "Moderate Stability"
            avg <= 50 && jitter <= 12 -> "Tournament Grade"
            else -> "Stable for Gaming"
        }

        PingTestReport(
            endpoint = endpoint,
            samplesMs = samples,
            avgMs = avg,
            minMs = min,
            maxMs = max,
            jitterMs = jitter,
            packetLossPercent = lossPct,
            stabilityLabel = stability,
            timestampMs = System.currentTimeMillis()
        )
    }
}
