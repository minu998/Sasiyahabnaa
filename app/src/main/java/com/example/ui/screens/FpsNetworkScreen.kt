package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PingEndpoint
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.NeonSparklineChart
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThermalCrimson
import com.example.ui.viewmodel.OptimizerUiState
import com.example.util.Formatters
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FpsNetworkScreen(
    uiState: OptimizerUiState,
    onStartFpsTracking: () -> Unit,
    onStopFpsTracking: () -> Unit,
    onResetFrameDrops: () -> Unit,
    onToggleSustainedMode: (Boolean) -> Unit,
    onToggleGamingDnd: (Boolean) -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenPowerSettings: () -> Unit,
    onSelectEndpoint: (PingEndpoint) -> Unit,
    onRunStabilityTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onStartFpsTracking()
        onDispose { onStopFpsTracking() }
    }

    val metrics = uiState.metrics

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "FPS & NETWORK OPTIMIZER",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Live Choreographer frame monitor, Gaming Mode & TCP latency test",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // FPS Monitor Card
            item {
                val maxHz = metrics.displayRefreshRateHz.coerceAtLeast(60f)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonGreen,
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("REAL-TIME UI FRAME MONITOR", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${metrics.liveUiFps.roundToInt()} FPS",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = NeonGreen
                                )
                                Text(
                                    text = "/ ${maxHz.roundToInt()} Hz panel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ElectricBlue,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onResetFrameDrops,
                            modifier = Modifier.testTag("reset_fps_drops_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset frame drop counter", tint = ElectricBlue)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Frame Drops: ${metrics.frameDropCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (metrics.frameDropCount > 5) CyberAmber else TextSecondary
                        )
                        Text(
                            text = "Modes: ${metrics.supportedRefreshRatesHz.joinToString("/") { "${it.roundToInt()}Hz" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    if (uiState.liveFpsSamples.size >= 2) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NeonSparklineChart(
                            values = uiState.liveFpsSamples,
                            lineColor = NeonGreen,
                            minRange = 15f,
                            maxRange = maxHz + 5f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Honest Telemetry: Measures live Android Choreographer frame pacing. Never fakes FPS unlocks or alters Free Fire internal files.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // CPU & Gaming Performance Mode Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = ElectricBlue,
                    cornerRadius = 18.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.DeveloperBoard, contentDescription = null, tint = ElectricBlue)
                        Text("PERFORMANCE & GAMING MODE", style = MaterialTheme.typography.labelLarge, color = ElectricBlue)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CPU Load", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("${metrics.cpuUsagePercent}% (${metrics.cpuCoreCount} cores)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Game Mode API", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(metrics.gameModeLabel, style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                        }
                    }

                    if (metrics.cpuCurrentFreqMhz.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val freqSummary = metrics.cpuCurrentFreqMhz.take(4).joinToString(" • ") { "${it} MHz" }
                        Text(
                            text = "Active Clocks: $freqSummary",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sustained Performance Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = if (metrics.isSustainedPerformanceSupported) {
                                    "Supported on this device for steady thermal clocks"
                                } else {
                                    "Not advertised by device OEM hardware"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.sustainedPerformanceEnabled,
                            enabled = metrics.isSustainedPerformanceSupported,
                            onCheckedChange = onToggleSustainedMode,
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen),
                            modifier = Modifier.testTag("switch_sustained_performance")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                Text("Gaming Do Not Disturb", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            Text(
                                text = if (metrics.isDndAccessGranted) {
                                    "Silences non-priority alerts during matches"
                                } else {
                                    "Requires Do Not Disturb access in Android Settings"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        if (metrics.isDndAccessGranted) {
                            Switch(
                                checked = uiState.gamingDndEnabled,
                                onCheckedChange = onToggleGamingDnd,
                                modifier = Modifier.testTag("switch_gaming_dnd")
                            )
                        } else {
                            OutlinedButton(
                                onClick = onOpenDndSettings,
                                border = BorderStroke(1.dp, ElectricBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("grant_dnd_access_button")
                            ) {
                                Text("Grant", color = ElectricBlue)
                            }
                        }
                    }

                    if (metrics.isPowerSaveMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenPowerSettings,
                            border = BorderStroke(1.dp, CyberAmber),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("disable_battery_saver_button")
                        ) {
                            Text("Battery Saver ON — Tap to Open Battery Settings", color = CyberAmber)
                        }
                    }
                }
            }

            // Network Ping Monitor & Internet Connection Stability Test
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = ElectricBlue,
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = NeonGreen)
                            Text("NETWORK PING & STABILITY", style = MaterialTheme.typography.labelLarge, color = NeonGreen)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (metrics.isOnline) NeonGreen.copy(alpha = 0.16f) else ThermalCrimson.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = metrics.networkTypeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (metrics.isOnline) NeonGreen else ThermalCrimson,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NetStatItem("Live Ping", metrics.lastPingMs?.let { "${it} ms" } ?: "--")
                        NetStatItem("Jitter", metrics.pingJitterMs?.let { "${it} ms" } ?: "--")
                        NetStatItem("Packet Loss", metrics.packetLossPercent?.let { "${it}%" } ?: "--")
                        NetStatItem("Downlink", Formatters.formatBandwidthKbps(metrics.downstreamKbps))
                    }

                    if (metrics.wifiLinkSpeedMbps != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Wi-Fi Link: ${metrics.wifiLinkSpeedMbps} Mbps • Signal: ${metrics.wifiRssiDbm ?: "--"} dBm",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("TEST ENDPOINT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.endpoints.forEach { ep ->
                            val selected = uiState.selectedEndpoint?.id == ep.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectEndpoint(ep) },
                                label = { Text(ep.host) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricBlue
                                ),
                                modifier = Modifier.testTag("ping_endpoint_${ep.id}")
                            )
                        }
                    }

                    if (uiState.livePingSamples.size >= 2) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NeonSparklineChart(
                            values = uiState.livePingSamples.map { it.toFloat() },
                            lineColor = ElectricBlue,
                            minRange = 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    NeonGradientButton(
                        text = if (uiState.isRunningPingTest) "TESTING 8 PACKETS..." else "RUN STABILITY TEST",
                        onClick = onRunStabilityTest,
                        icon = Icons.Rounded.NetworkPing,
                        isLoading = uiState.isRunningPingTest,
                        gradientColors = listOf(ElectricBlue, NeonGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_stability_test_button")
                    )

                    uiState.pingReport?.let { report ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianBlack.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, GlassBorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Result: ${report.stabilityLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NeonGreen
                                )
                                Text(
                                    text = "Avg: ${report.avgMs ?: "--"} ms • Min: ${report.minMs ?: "--"} ms • Max: ${report.maxMs ?: "--"} ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Jitter: ${report.jitterMs ?: "--"} ms • Loss: ${report.packetLossPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetStatItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}
