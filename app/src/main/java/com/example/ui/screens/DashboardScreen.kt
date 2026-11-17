package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.OptimizationProfileEntity
import com.example.data.model.GameTargetType
import com.example.data.model.ThermalLevel
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.PerformanceArcGauge
import com.example.ui.components.TelemetryMetricCard
import com.example.ui.components.ThermalWarningBanner
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThermalCrimson
import com.example.ui.theme.VioletPulse
import com.example.ui.viewmodel.OptimizerUiState
import com.example.util.Formatters
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    uiState: OptimizerUiState,
    activeProfile: OptimizationProfileEntity?,
    onStartBoost: () -> Unit,
    onOpenGameSelection: () -> Unit,
    onOpenCacheCleaner: () -> Unit,
    onOpenRamAnalyzer: () -> Unit,
    onOpenThermalMonitor: () -> Unit,
    onOpenFpsMonitor: () -> Unit,
    onOpenGameSettings: () -> Unit,
    onOpenProfilesHistory: () -> Unit,
    onToggleSessionTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = uiState.metrics
    val selectedGame = uiState.selectedGame
    val isOverheating = metrics.thermalLevel == ThermalLevel.OVERHEATING ||
        metrics.thermalLevel == ThermalLevel.CRITICAL ||
        metrics.batteryTempCelsius >= uiState.thermalThresholdCelsius

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Session Timer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.header_brand),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Real-Time Device Telemetry & Game Launcher",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onToggleSessionTimer,
                            shape = RoundedCornerShape(20.dp),
                            color = if (uiState.sessionRunning) NeonGreen.copy(alpha = 0.16f) else CarbonCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.sessionRunning) NeonGreen else GlassBorderSubtle
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("session_timer_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.sessionRunning) Icons.Rounded.Stop else Icons.Rounded.Timer,
                                    contentDescription = "Session timer",
                                    tint = if (uiState.sessionRunning) NeonGreen else ElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (uiState.sessionRunning) {
                                        Formatters.formatDurationHms(uiState.sessionElapsedMs)
                                    } else {
                                        "Timer"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.sessionRunning) NeonGreen else TextPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenGameSettings,
                            modifier = Modifier.testTag("header_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Game Settings",
                                tint = ElectricBlue
                            )
                        }
                    }
                }
            }

            // Overheating Alert Banner
            item {
                ThermalWarningBanner(
                    visible = isOverheating,
                    temperatureText = Formatters.formatTemperature(metrics.batteryTempCelsius, uiState.useFahrenheit),
                    thermalStatusLabel = metrics.thermalStatusLabel,
                    onOpenThermal = onOpenThermalMonitor
                )
            }

            // Hero Target Game + START GAME BOOST
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(NeonGreen.copy(alpha = 0.6f), ElectricBlue.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_gaming_core),
                        contentDescription = "Gaming optimizer hero artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.38f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianBlack.copy(alpha = 0.45f),
                                        ObsidianBlack.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val emblemRes = when (selectedGame?.targetType) {
                                GameTargetType.FREE_FIRE_MAX -> R.drawable.img_ffmax_emblem
                                else -> R.drawable.img_ff_emblem
                            }
                            Image(
                                painter = painterResource(id = emblemRes),
                                contentDescription = selectedGame?.displayName ?: "Free Fire",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, NeonGreen.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = selectedGame?.displayName ?: "Free Fire",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val installed = selectedGame?.isInstalled == true
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (installed) NeonGreen.copy(alpha = 0.16f) else CyberAmber.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = if (installed) "INSTALLED" else "NOT FOUND",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (installed) NeonGreen else CyberAmber,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Profile: ${activeProfile?.name ?: "Competitive Ranked"} • ${metrics.gameModeLabel} Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ElectricBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onOpenGameSelection,
                                modifier = Modifier
                                    .background(CarbonCard.copy(alpha = 0.85f), CircleShape)
                                    .testTag("switch_game_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = "Select game",
                                    tint = NeonGreen
                                )
                            }
                        }

                        NeonGradientButton(
                            text = "START GAME BOOST",
                            onClick = onStartBoost,
                            icon = Icons.Rounded.RocketLaunch,
                            isLoading = uiState.isBoostInProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_game_boost_button")
                        )
                    }
                }
            }

            // Device Performance Score Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonGreen,
                    cornerRadius = 22.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PerformanceArcGauge(
                            score = metrics.performanceScore,
                            statusLabel = metrics.performanceStatus,
                            size = 144.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DEVICE PERFORMANCE",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricBlue
                            )
                            Text(
                                text = metrics.performanceStatus,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            StatusMiniRow(
                                label = "Display",
                                value = "${metrics.displayRefreshRateHz.roundToInt()} Hz (${metrics.liveUiFps.roundToInt()} FPS)",
                                color = NeonGreen
                            )
                            StatusMiniRow(
                                label = "Battery",
                                value = "${metrics.batteryLevel}% • ${metrics.batteryPluggedLabel}",
                                color = if (metrics.isPowerSaveMode) CyberAmber else ElectricBlue
                            )
                            StatusMiniRow(
                                label = "Reclaimable",
                                value = Formatters.formatBytes(uiState.cacheScan.totalReclaimableBytes),
                                color = CyberAmber
                            )
                        }
                    }
                }
            }

            // Core 6-Metric Live Telemetry Grid (2 columns)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TelemetryMetricCard(
                            title = "RAM Usage",
                            value = "${metrics.ramUsedPercent}%",
                            subtitle = "${metrics.ramAvailableMb} MB free / ${metrics.ramTotalMb} MB",
                            icon = Icons.Rounded.Memory,
                            accentColor = ElectricBlue,
                            progressFraction = metrics.ramUsedPercent / 100f,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_ram_card",
                            onClick = onOpenRamAnalyzer
                        )
                        TelemetryMetricCard(
                            title = "CPU Usage",
                            value = "${metrics.cpuUsagePercent}%",
                            subtitle = "${metrics.cpuCoreCount} cores • ${metrics.cpuAbi}",
                            icon = Icons.Rounded.DeveloperBoard,
                            accentColor = NeonGreen,
                            progressFraction = metrics.cpuUsagePercent / 100f,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_cpu_card",
                            onClick = onOpenFpsMonitor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val tempColor = when (metrics.thermalLevel) {
                            ThermalLevel.OPTIMAL -> NeonGreen
                            ThermalLevel.WARM -> ElectricBlue
                            ThermalLevel.ELEVATED -> CyberAmber
                            else -> ThermalCrimson
                        }
                        TelemetryMetricCard(
                            title = "Battery Temp",
                            value = Formatters.formatTemperature(metrics.batteryTempCelsius, uiState.useFahrenheit),
                            subtitle = metrics.thermalStatusLabel,
                            icon = Icons.Rounded.Thermostat,
                            accentColor = tempColor,
                            progressFraction = ((metrics.batteryTempCelsius - 20f) / 30f).coerceIn(0f, 1f),
                            modifier = Modifier.weight(1f),
                            testTag = "metric_temp_card",
                            onClick = onOpenThermalMonitor
                        )
                        val storageUsedRatio = if (metrics.storageTotalBytes > 0L) {
                            metrics.storageUsedBytes.toFloat() / metrics.storageTotalBytes.toFloat()
                        } else 0f
                        TelemetryMetricCard(
                            title = "Free Storage",
                            value = Formatters.formatBytes(metrics.storageAvailableBytes),
                            subtitle = "of ${Formatters.formatBytes(metrics.storageTotalBytes)}",
                            icon = Icons.Rounded.Storage,
                            accentColor = VioletPulse,
                            progressFraction = storageUsedRatio,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_storage_card",
                            onClick = onOpenCacheCleaner
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val pingText = metrics.lastPingMs?.let { "${it} ms" } ?: "-- ms"
                        val pingColor = when {
                            metrics.lastPingMs == null -> TextSecondary
                            metrics.lastPingMs <= 55 -> NeonGreen
                            metrics.lastPingMs <= 110 -> ElectricBlue
                            else -> CyberAmber
                        }
                        TelemetryMetricCard(
                            title = "Network Ping",
                            value = pingText,
                            subtitle = metrics.networkTypeLabel,
                            icon = Icons.Rounded.NetworkPing,
                            accentColor = pingColor,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_ping_card",
                            onClick = onOpenFpsMonitor
                        )
                        TelemetryMetricCard(
                            title = "UI FPS Monitor",
                            value = "${metrics.liveUiFps.roundToInt()} FPS",
                            subtitle = "Panel ${metrics.displayRefreshRateHz.roundToInt()} Hz • ${metrics.frameDropCount} drops",
                            icon = Icons.Rounded.Speed,
                            accentColor = NeonGreen,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_fps_card",
                            onClick = onOpenFpsMonitor
                        )
                    }
                }
            }

            // Quick Action Command Deck (Additional Buttons required by prompt)
            item {
                Text(
                    text = "OPTIMIZATION MODULES",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            label = "CLEAN CACHE",
                            sublabel = Formatters.formatBytes(uiState.cacheScan.totalReclaimableBytes),
                            icon = Icons.Rounded.CleaningServices,
                            accentColor = NeonGreen,
                            onClick = onOpenCacheCleaner,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_clean_cache"
                        )
                        QuickActionButton(
                            label = "RAM ANALYZER",
                            sublabel = "${metrics.ramAvailableMb} MB free",
                            icon = Icons.Rounded.Memory,
                            accentColor = ElectricBlue,
                            onClick = onOpenRamAnalyzer,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_ram_analyzer"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            label = "TEMP MONITOR",
                            sublabel = metrics.thermalLevel.label,
                            icon = Icons.Rounded.Thermostat,
                            accentColor = CyberAmber,
                            onClick = onOpenThermalMonitor,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_temperature_monitor"
                        )
                        QuickActionButton(
                            label = "FPS MONITOR",
                            sublabel = "${metrics.displayRefreshRateHz.roundToInt()} Hz Panel",
                            icon = Icons.Rounded.Speed,
                            accentColor = NeonGreen,
                            onClick = onOpenFpsMonitor,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_fps_monitor"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            label = "GAME SETTINGS",
                            sublabel = "Modes & Device Info",
                            icon = Icons.Rounded.Tune,
                            accentColor = ElectricBlue,
                            onClick = onOpenGameSettings,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_game_settings"
                        )
                        QuickActionButton(
                            label = "PROFILES & LOGS",
                            sublabel = activeProfile?.name ?: "Presets",
                            icon = Icons.Rounded.History,
                            accentColor = VioletPulse,
                            onClick = onOpenProfilesHistory,
                            modifier = Modifier.weight(1f),
                            testTag = "btn_profiles_history"
                        )
                    }
                }
            }

            // Game Selection shortcut & Safety Disclaimer
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = GlassBorderSubtle,
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Gamepad,
                                contentDescription = null,
                                tint = ElectricBlue
                            )
                            Column {
                                Text(
                                    text = "Game Selection System",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Free Fire, Free Fire MAX & installed games",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        TextButton(
                            onClick = onOpenGameSelection,
                            modifier = Modifier.testTag("open_game_selection_text_button")
                        ) {
                            Text("Select", color = NeonGreen)
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.disclaimer_independent),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusMiniRow(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    sublabel: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(CarbonCard.copy(alpha = 0.92f), shape)
            .border(1.dp, accentColor.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
