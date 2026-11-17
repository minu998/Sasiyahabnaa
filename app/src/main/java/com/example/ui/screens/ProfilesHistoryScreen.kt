package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.OptimizationHistoryEntity
import com.example.data.local.OptimizationProfileEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThermalCrimson
import com.example.util.Formatters

@Composable
fun ProfilesHistoryScreen(
    profiles: List<OptimizationProfileEntity>,
    history: List<OptimizationHistoryEntity>,
    activeProfileId: Long,
    useFahrenheit: Boolean,
    onSelectProfile: (OptimizationProfileEntity) -> Unit,
    onCreateProfile: (String, String, Boolean, Boolean, Boolean, Boolean, Float, String) -> Unit,
    onDeleteCustomProfile: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profiles_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Column {
                        Text("PROFILES & HISTORY", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text("Custom optimization presets & boost logs", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("add_custom_profile_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create custom profile", tint = NeonGreen)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = ElectricBlue)
                        Text("OPTIMIZATION PROFILES", style = MaterialTheme.typography.labelLarge, color = ElectricBlue)
                    }
                }

                items(profiles, key = { it.id }) { profile ->
                    val isSelected = profile.id == activeProfileId
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_card_${profile.id}"),
                        accentColor = if (isSelected) NeonGreen else GlassBorderSubtle,
                        cornerRadius = 16.dp,
                        onClick = { onSelectProfile(profile) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(profile.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    if (isSelected) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = NeonGreen)
                                    }
                                }
                                Text(profile.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Preset: ${profile.recommendedGraphicsPreset} • Thermal limit ${Formatters.formatTemperature(profile.thermalAlertThresholdC, useFahrenheit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricBlue
                                )
                            }
                            if (!profile.isBuiltIn) {
                                IconButton(
                                    onClick = { onDeleteCustomProfile(profile.id) },
                                    modifier = Modifier.testTag("delete_profile_${profile.id}")
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete profile", tint = ThermalCrimson)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = NeonGreen)
                            Text("OPTIMIZATION HISTORY (${history.size})", style = MaterialTheme.typography.labelLarge, color = NeonGreen)
                        }
                        if (history.isNotEmpty()) {
                            TextButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Text("Clear All", color = ThermalCrimson)
                            }
                        }
                    }
                }

                if (history.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = GlassBorderSubtle,
                            cornerRadius = 16.dp
                        ) {
                            Text(
                                text = "No boost sessions recorded yet. Run START GAME BOOST on the Dashboard to log real pre-launch diagnostics.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(history, key = { it.id }) { entry ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = ElectricBlue,
                            cornerRadius = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.gameName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeonGreen.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "Score ${entry.scoreBefore} → ${entry.scoreAfter}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${entry.profileName} • ${Formatters.formatTimestamp(entry.timestampMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "RAM: ${entry.ramAvailableMb} MB free • Temp: ${Formatters.formatTemperature(entry.batteryTempCelsius, useFahrenheit)} • Cache: ${Formatters.formatBytes(entry.cacheClearedBytes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var autoCache by remember { mutableStateOf(true) }
        var sustained by remember { mutableStateOf(true) }
        var dnd by remember { mutableStateOf(false) }
        var netCheck by remember { mutableStateOf(true) }
        var threshold by remember { mutableFloatStateOf(40.0f) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Create Optimization Profile", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_name")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ToggleRow("Auto-clean accessible cache", autoCache) { autoCache = it }
                    ToggleRow("Sustained Performance hint", sustained) { sustained = it }
                    ToggleRow("Gaming Do Not Disturb", dnd) { dnd = it }
                    ToggleRow("Network ping pre-check", netCheck) { netCheck = it }
                    Text(
                        text = "Thermal Alert Limit: ${Formatters.formatTemperature(threshold, useFahrenheit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber
                    )
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 36f..45f
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreateProfile(
                                name,
                                desc,
                                autoCache,
                                sustained,
                                dnd,
                                netCheck,
                                threshold,
                                "Custom Preset"
                            )
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_custom_profile_button")
                ) {
                    Text("Save", color = NeonGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
