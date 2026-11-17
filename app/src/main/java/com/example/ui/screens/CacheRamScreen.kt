package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPulse
import com.example.ui.viewmodel.OptimizerUiState
import com.example.util.Formatters

@Composable
fun CacheRamScreen(
    uiState: OptimizerUiState,
    initialTab: Int = 0,
    onScanCache: () -> Unit,
    onCleanCache: () -> Unit,
    onSetSafFolderUri: (String?) -> Unit,
    onOpenSystemCacheCleaner: () -> Unit,
    onOpenGameStorageSettings: () -> Unit,
    onOptimizeRamSafely: () -> Unit,
    onRefreshBackgroundApps: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAppInfo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onSetSafFolderUri(uri.toString())
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "CACHE & RAM BOOSTER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Safe accessible cache cleaning, storage analyzer & memory booster",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianBlack,
                contentColor = NeonGreen
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Cache & Storage") },
                    modifier = Modifier.testTag("tab_cache_storage")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("RAM & Background") },
                    modifier = Modifier.testTag("tab_ram_background")
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Storage Analyzer Card
                    item {
                        val m = uiState.metrics
                        val usedFraction = if (m.storageTotalBytes > 0L) {
                            (m.storageUsedBytes.toFloat() / m.storageTotalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = VioletPulse,
                            cornerRadius = 20.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = VioletPulse)
                                    Text("STORAGE ANALYZER", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                }
                                Text(
                                    text = "${(usedFraction * 100).toInt()}% Used",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VioletPulse
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { usedFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = VioletPulse,
                                trackColor = GlassBorderSubtle.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Available: ${Formatters.formatBytes(m.storageAvailableBytes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonGreen
                                )
                                Text(
                                    text = "Total: ${Formatters.formatBytes(m.storageTotalBytes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Safe Mode Cache Cleaner Card
                    item {
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
                                    Text(
                                        text = "RECLAIMABLE ACCESSIBLE CACHE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = Formatters.formatBytes(uiState.cacheScan.totalReclaimableBytes),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = NeonGreen
                                    )
                                }
                                IconButton(
                                    onClick = onScanCache,
                                    modifier = Modifier.testTag("rescan_cache_button")
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Rescan cache", tint = ElectricBlue)
                                }
                            }
                            if (uiState.cacheScan.lastCleanedBytes > 0L) {
                                Text(
                                    text = "Last clean freed: ${Formatters.formatBytes(uiState.cacheScan.lastCleanedBytes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            NeonGradientButton(
                                text = "CLEAN CACHE",
                                onClick = onCleanCache,
                                icon = Icons.Rounded.CleaningServices,
                                isLoading = uiState.isCleaningCache || uiState.isScanningCache,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("clean_cache_action_button")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Safe Mode: Never touches game data, account login, maps, OBB, or protected Android directories.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Breakdown of scanned items
                    if (uiState.cacheScan.items.isNotEmpty()) {
                        item {
                            Text(
                                text = "SCANNED ACCESSIBLE ITEMS",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        items(uiState.cacheScan.items, key = { it.id }) { item ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = GlassBorderSubtle,
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(
                                            text = "${item.category.label} • ${item.pathDisplay}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Text(
                                        text = Formatters.formatBytes(item.sizeBytes),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricBlue
                                    )
                                }
                            }
                        }
                    }

                    // Android-Supported Deep Cache & SAF Folder Tools
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = ElectricBlue,
                            cornerRadius = 18.dp
                        ) {
                            Text(
                                text = "ANDROID SYSTEM CACHE TOOLS",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Due to Android Scoped Storage security, clearing other apps' private cache requires Android's built-in Storage tools or an explicitly chosen folder.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onOpenSystemCacheCleaner,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("system_cache_cleaner_button"),
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                Text("  Open Android System Cache Cleaner", color = TextPrimary)
                            }
                            if (uiState.selectedGame?.isInstalled == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onOpenGameStorageSettings,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("game_storage_settings_button"),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                    Text("  Open ${uiState.selectedGame.displayName} App Storage", color = TextPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { folderPickerLauncher.launch(null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("saf_folder_pick_button"),
                                border = BorderStroke(1.dp, GlassBorderSubtle),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(18.dp))
                                Text(
                                    text = uiState.cacheScan.safFolderName?.let {
                                        "  Folder Linked: $it (Scan .tmp/.log)"
                                    } ?: "  Select Temp Folder via Android File Picker",
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                // RAM Booster & Background Activity Monitor Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        val m = uiState.metrics
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Memory, contentDescription = null, tint = ElectricBlue)
                                    Text("REAL-TIME RAM ANALYZER", style = MaterialTheme.typography.labelLarge, color = ElectricBlue)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (m.isLowMemory) CyberAmber.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = if (m.isLowMemory) "LOW MEMORY" else "HEALTHY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (m.isLowMemory) CyberAmber else NeonGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                RamStatColumn("Available RAM", "${m.ramAvailableMb} MB", NeonGreen)
                                RamStatColumn("Used RAM", "${m.ramUsedMb} MB", ElectricBlue)
                                RamStatColumn("Total RAM", "${m.ramTotalMb} MB", TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (m.ramUsedPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (m.ramUsedPercent > 82) CyberAmber else ElectricBlue,
                                trackColor = GlassBorderSubtle.copy(alpha = 0.45f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format(
                                    "Optimizer Heap: %.1f MB / %.0f MB max",
                                    m.appHeapUsedMb,
                                    m.appHeapMaxMb
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            NeonGradientButton(
                                text = "OPTIMIZE MEMORY SAFELY",
                                onClick = onOptimizeRamSafely,
                                icon = Icons.Rounded.Memory,
                                isLoading = uiState.isOptimizingRam,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("optimize_ram_button")
                            )
                            if (uiState.ramOptimizationMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = uiState.ramOptimizationMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    // Background Activity Monitor Header
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = if (uiState.metrics.isUsageAccessGranted) NeonGreen else CyberAmber,
                            cornerRadius = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "BACKGROUND ACTIVITY MONITOR",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (uiState.metrics.isUsageAccessGranted) {
                                            "Usage Access granted. Critical system apps are protected."
                                        } else {
                                            "Grant Usage Access in Android Settings to inspect recent heavy background apps."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = onRefreshBackgroundApps,
                                    modifier = Modifier.testTag("refresh_bg_apps_button")
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh background apps", tint = ElectricBlue)
                                }
                            }
                            if (!uiState.metrics.isUsageAccessGranted) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onOpenUsageAccessSettings,
                                    border = BorderStroke(1.dp, CyberAmber),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("grant_usage_access_button")
                                ) {
                                    Text("Grant Usage Access in Settings", color = CyberAmber)
                                }
                            }
                        }
                    }

                    items(uiState.backgroundApps, key = { it.packageName }) { app ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = if (app.isSystemCritical) GlassBorderSubtle else ElectricBlue,
                            cornerRadius = 14.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(CarbonCard, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (app.isSystemCritical) Icons.Rounded.Lock else Icons.Rounded.Apps,
                                        contentDescription = app.appName,
                                        tint = if (app.isSystemCritical) TextMuted else ElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (app.foregroundMinutes > 0) {
                                            "${app.importanceLabel} • ${app.foregroundMinutes}m recent"
                                        } else {
                                            app.importanceLabel
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (app.isSystemCritical) TextMuted else TextSecondary
                                    )
                                }
                                if (app.isSystemCritical) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GlassBorderSubtle.copy(alpha = 0.35f)
                                    ) {
                                        Text(
                                            text = "PROTECTED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    TextButton(
                                        onClick = { onOpenAppInfo(app.packageName) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen),
                                        modifier = Modifier.testTag("manage_bg_app_${app.packageName}")
                                    ) {
                                        Text("App Info")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RamStatColumn(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
    }
}
