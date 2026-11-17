package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.OptimizerUiState
import com.example.util.Formatters

@Composable
fun DeviceSettingsScreen(
    uiState: OptimizerUiState,
    onToggleFahrenheit: (Boolean) -> Unit,
    onToggleSustainedPerformance: (Boolean) -> Unit,
    onToggleOverheatingAlerts: (Boolean) -> Unit,
    onStartOrResetTimer: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenDndAccess: () -> Unit,
    onOpenProfilesHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spec = uiState.hardwareSpec
    val metrics = uiState.metrics

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text("GAME SETTINGS & DEVICE INFO", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Hardware specifications, system permissions & safety", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Gaming Session Timer & Quick Toggles
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = NeonGreen,
                        cornerRadius = 18.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = NeonGreen)
                            Text("GAMING SESSION & PREFERENCES", style = MaterialTheme.typography.labelLarge, color = NeonGreen)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Gaming Session Timer", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = if (uiState.sessionRunning) {
                                        "Active: ${Formatters.formatDurationHms(uiState.sessionElapsedMs)}"
                                    } else "Stopped",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ElectricBlue
                                )
                            }
                            OutlinedButton(
                                onClick = onStartOrResetTimer,
                                border = BorderStroke(1.dp, NeonGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("settings_timer_button")
                            ) {
                                Text(if (uiState.sessionRunning) "Reset" else "Start", color = NeonGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        SettingSwitchRow(
                            title = "Sustained Performance Mode",
                            subtitle = "Hint Android thermal governor for steady gaming clocks",
                            checked = uiState.sustainedPerformanceEnabled,
                            enabled = metrics.isSustainedPerformanceSupported,
                            onCheckedChange = onToggleSustainedPerformance
                        )
                        SettingSwitchRow(
                            title = "Automatic Overheating Alerts",
                            subtitle = "Trigger warning when exceeding thermal limit",
                            checked = uiState.overheatingAlertsEnabled,
                            enabled = true,
                            onCheckedChange = onToggleOverheatingAlerts
                        )
                        SettingSwitchRow(
                            title = "Temperature in Fahrenheit (°F)",
                            subtitle = "Switch between Celsius and Fahrenheit",
                            checked = uiState.useFahrenheit,
                            enabled = true,
                            onCheckedChange = onToggleFahrenheit
                        )
                    }
                }

                // Permissions & Profiles Shortcut
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = ElectricBlue,
                        cornerRadius = 18.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = ElectricBlue)
                            Text("PERMISSIONS & PROFILES", style = MaterialTheme.typography.labelLarge, color = ElectricBlue)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onOpenProfilesHistory,
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_open_profiles_button")
                        ) {
                            Text("Manage Custom Optimization Profiles & History", color = NeonGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onOpenUsageAccess,
                            border = BorderStroke(1.dp, if (metrics.isUsageAccessGranted) GlassBorderSubtle else CyberAmber),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_usage_access_button")
                        ) {
                            Text(
                                text = if (metrics.isUsageAccessGranted) "Usage Access: Granted" else "Grant Usage Access (Background Monitor)",
                                color = if (metrics.isUsageAccessGranted) TextSecondary else CyberAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onOpenDndAccess,
                            border = BorderStroke(1.dp, if (metrics.isDndAccessGranted) GlassBorderSubtle else ElectricBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_dnd_access_button")
                        ) {
                            Text(
                                text = if (metrics.isDndAccessGranted) "Do Not Disturb Access: Granted" else "Grant DND Access (Gaming Mode)",
                                color = if (metrics.isDndAccessGranted) TextSecondary else ElectricBlue
                            )
                        }
                    }
                }

                // Device Information
                if (spec != null) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = ElectricBlue,
                            cornerRadius = 18.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = ElectricBlue)
                                Text("DEVICE INFORMATION", style = MaterialTheme.typography.labelLarge, color = ElectricBlue)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            DeviceSpecRow("Model", "${spec.manufacturer} ${spec.model} (${spec.deviceName})")
                            DeviceSpecRow("Android OS", "Android ${spec.androidVersion} (API ${spec.apiLevel})")
                            DeviceSpecRow("Security Patch", spec.securityPatch)
                            DeviceSpecRow("SoC / Hardware", spec.boardHardware)
                            DeviceSpecRow("CPU Architecture", "${spec.cpuCores} Cores • ${spec.supportedAbis}")
                            DeviceSpecRow("Total RAM", "${spec.totalRamMb} MB")
                            DeviceSpecRow("Internal Storage", Formatters.formatBytes(spec.totalStorageBytes))
                            DeviceSpecRow("Display", "${spec.screenResolution} (${spec.screenDensityDpi} dpi)")
                            DeviceSpecRow("Refresh Rates", spec.displayRefreshRates)
                            DeviceSpecRow("Sustained Mode API", if (spec.sustainedPerformanceSupported) "Supported" else "Not Supported")
                            DeviceSpecRow("GameManager API", if (spec.gameManagerSupported) "Android 12+ Available" else "Legacy OS")
                        }
                    }
                }

                // Privacy & Anti-Cheat Guarantee
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = NeonGreen,
                        cornerRadius = 18.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = NeonGreen)
                            Text("ANTI-CHEAT & PRIVACY GUARANTEE", style = MaterialTheme.typography.labelLarge, color = NeonGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• No root access required or used.\n" +
                                "• Zero game file, OBB, or memory modification.\n" +
                                "• Zero code injection or anti-cheat tampering.\n" +
                                "• All device telemetry stays 100% local on your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) TextPrimary else TextMuted)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DeviceSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
    }
}
