package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ThermalLevel
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonSparklineChart
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThermalCrimson
import com.example.ui.viewmodel.OptimizerUiState
import com.example.util.Formatters

@Composable
fun ThermalMonitorScreen(
    uiState: OptimizerUiState,
    onToggleAlerts: (Boolean) -> Unit,
    onChangeThreshold: (Float) -> Unit,
    onToggleFahrenheit: (Boolean) -> Unit,
    onOpenBackgroundMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = uiState.metrics
    val tempColor = when (metrics.thermalLevel) {
        ThermalLevel.OPTIMAL -> NeonGreen
        ThermalLevel.WARM -> ElectricBlue
        ThermalLevel.ELEVATED -> CyberAmber
        ThermalLevel.OVERHEATING, ThermalLevel.CRITICAL -> ThermalCrimson
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onToggleAlerts(true)
    }

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
                        text = "DEVICE TEMPERATURE MONITOR",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Battery thermal sensor, PowerManager status & cooling guidance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Hero Thermal Gauge + Live Sparkline
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = tempColor,
                    cornerRadius = 22.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BATTERY TEMPERATURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = Formatters.formatTemperature(metrics.batteryTempCelsius, uiState.useFahrenheit),
                                style = MaterialTheme.typography.displayLarge,
                                color = tempColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tempColor.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "${metrics.thermalLevel.label} • ${metrics.thermalStatusLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tempColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (metrics.thermalLevel == ThermalLevel.OVERHEATING || metrics.thermalLevel == ThermalLevel.CRITICAL) {
                                Icons.Rounded.Warning
                            } else {
                                Icons.Rounded.Thermostat
                            },
                            contentDescription = "Temperature status",
                            tint = tempColor,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    if (uiState.liveTempSamples.size >= 2) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "LIVE THERMAL TREND",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        NeonSparklineChart(
                            values = uiState.liveTempSamples,
                            lineColor = tempColor,
                            minRange = 22f,
                            maxRange = 50f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        )
                    }
                }
            }

            // Battery Status Monitor
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
                        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = ElectricBlue)
                        Text("BATTERY STATUS MONITOR", style = MaterialTheme.typography.labelLarge, color = ElectricBlue)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BatteryDetailCell("Charge", "${metrics.batteryLevel}%")
                        BatteryDetailCell("Power Source", metrics.batteryPluggedLabel)
                        BatteryDetailCell("Health", metrics.batteryHealthLabel)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BatteryDetailCell("Voltage", "${metrics.batteryVoltageMv} mV")
                        BatteryDetailCell("Chemistry", metrics.batteryTechnology)
                        BatteryDetailCell("Unit", if (uiState.useFahrenheit) "°F" else "°C")
                    }
                }
            }

            // Cooling Recommendations
            item {
                val recommendations = buildCoolingRecommendations(
                    tempC = metrics.batteryTempCelsius,
                    isCharging = metrics.batteryIsCharging,
                    backgroundAppCount = uiState.backgroundApps.count { !it.isSystemCritical },
                    thermalLevel = metrics.thermalLevel
                )
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = NeonGreen,
                    cornerRadius = 18.dp,
                    onClick = onOpenBackgroundMonitor
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = NeonGreen)
                        Text("SMART COOLING RECOMMENDATIONS", style = MaterialTheme.typography.labelLarge, color = NeonGreen)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    recommendations.forEach { rec ->
                        Text(
                            text = "• $rec",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Note: Hardware thermal protection is controlled by Android OS and is never bypassed.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Automatic Overheating Alerts & Unit Settings
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = CyberAmber,
                    cornerRadius = 18.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = CyberAmber)
                        Text("OVERHEATING ALERT CONTROLS", style = MaterialTheme.typography.labelLarge, color = CyberAmber)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Overheating Alerts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = "Notify when battery exceeds alert threshold",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.overheatingAlertsEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleAlerts(wantEnable)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen),
                            modifier = Modifier.testTag("switch_overheating_alerts")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Alert Threshold: ${Formatters.formatTemperature(uiState.thermalThresholdCelsius, uiState.useFahrenheit)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlue
                    )
                    Slider(
                        value = uiState.thermalThresholdCelsius,
                        onValueChange = onChangeThreshold,
                        valueRange = 36f..46f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberAmber,
                            activeTrackColor = CyberAmber
                        ),
                        modifier = Modifier.testTag("slider_thermal_threshold")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Display Temperature in Fahrenheit (°F)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Switch(
                            checked = uiState.useFahrenheit,
                            onCheckedChange = onToggleFahrenheit,
                            modifier = Modifier.testTag("switch_temp_fahrenheit")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryDetailCell(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}

private fun buildCoolingRecommendations(
    tempC: Float,
    isCharging: Boolean,
    backgroundAppCount: Int,
    thermalLevel: ThermalLevel
): List<String> {
    val list = mutableListOf<String>()
    if (isCharging) {
        list.add("Unplug charger before entering Ranked/Clash Squad matches to prevent fast-charge heat stacking.")
    }
    if (tempC >= 40f || thermalLevel == ThermalLevel.OVERHEATING || thermalLevel == ThermalLevel.CRITICAL) {
        list.add("Take a 5–10 minute cooldown break and remove thick protective phone cases.")
        list.add("Switch Free Fire Graphics to 'Smooth' and disable Shadows to cut GPU thermal output.")
        list.add("Lower screen brightness below 65% and reduce volume or use wired earbuds.")
    } else if (tempC >= 36.5f) {
        list.add("Warm operating range. Avoid direct sunlight and keep brightness moderate.")
    } else {
        list.add("Battery temperature is cool and ideal for sustained high-FPS Free Fire gameplay.")
    }
    if (backgroundAppCount > 3) {
        list.add("$backgroundAppCount non-system background apps recently active—tap here to review & restrict background battery use.")
    }
    return list
}
