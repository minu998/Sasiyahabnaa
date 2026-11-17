package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GameEntry
import com.example.data.model.GameTargetType
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.OptimizerUiState

@Composable
fun GameSelectionScreen(
    uiState: OptimizerUiState,
    onSelectGame: (String) -> Unit,
    onAddManualGame: (String) -> Unit,
    onLoadManualPicker: () -> Unit,
    onRefreshGames: () -> Unit,
    onStartOptimization: () -> Unit,
    onInstallGame: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
                        modifier = Modifier.testTag("game_select_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "GAME SELECTION SYSTEM",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose Free Fire, Free Fire MAX, or installed games",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshGames,
                    modifier = Modifier.testTag("refresh_games_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh installed games",
                        tint = ElectricBlue
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "PRIMARY FREE FIRE TARGETS",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonGreen
                    )
                }

                items(uiState.primaryGames, key = { it.packageName }) { game ->
                    val isSelected = uiState.selectedGame?.packageName == game.packageName
                    PrimaryGameCard(
                        game = game,
                        isSelected = isSelected,
                        onSelect = { onSelectGame(game.packageName) },
                        onInstall = onInstallGame
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DETECTED INSTALLED GAMES (${uiState.detectedGames.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricBlue
                        )
                        TextButton(
                            onClick = {
                                onLoadManualPicker()
                                showManualPicker = true
                            },
                            modifier = Modifier.testTag("manual_game_select_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(" Manual Pick", color = NeonGreen)
                        }
                    }
                }

                if (uiState.detectedGames.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = GlassBorderSubtle,
                            cornerRadius = 16.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Gamepad,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No additional Android games auto-detected",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Tap Manual Pick above to add any installed game or app manually.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.detectedGames, key = { it.packageName }) { game ->
                        val isSelected = uiState.selectedGame?.packageName == game.packageName
                        DetectedGameRow(
                            game = game,
                            isSelected = isSelected,
                            onClick = { onSelectGame(game.packageName) }
                        )
                    }
                }
            }

            // Bottom Large START OPTIMIZATION button
            Surface(
                color = ObsidianSurface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val target = uiState.selectedGame
                    Text(
                        text = if (target != null) {
                            "Selected: ${target.displayName} • " +
                                if (target.isInstalled) "Installed & Ready" else "Not Installed"
                        } else "Select a target game",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    NeonGradientButton(
                        text = "START OPTIMIZATION",
                        onClick = onStartOptimization,
                        icon = Icons.Rounded.RocketLaunch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_select_start_optimization_button")
                    )
                }
            }
        }
    }

    if (showManualPicker) {
        val filtered = remember(uiState.allAppsForPicker, searchQuery) {
            if (searchQuery.isBlank()) uiState.allAppsForPicker
            else uiState.allAppsForPicker.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { showManualPicker = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = "Manual Game Selection",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CarbonCard)
                                    .clickable {
                                        onAddManualGame(app.packageName)
                                        showManualPicker = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DrawableIconBox(app.appIcon, app.displayName)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualPicker = false }) {
                    Text("Close", color = ElectricBlue)
                }
            }
        )
    }
}

@Composable
private fun PrimaryGameCard(
    game: GameEntry,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onInstall: () -> Unit
) {
    val accent = if (isSelected) NeonGreen else ElectricBlue
    val emblemRes = if (game.targetType == GameTargetType.FREE_FIRE_MAX) {
        R.drawable.img_ffmax_emblem
    } else {
        R.drawable.img_ff_emblem
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("primary_game_${game.packageName}"),
        accentColor = accent,
        cornerRadius = 18.dp,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = emblemRes),
                contentDescription = game.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = accent,
                        shape = RoundedCornerShape(14.dp)
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = game.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = game.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (game.isInstalled) NeonGreen.copy(alpha = 0.16f) else CyberAmber.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (game.isInstalled) {
                                "Installed v${game.versionName ?: "1.0"}"
                            } else {
                                "Not Installed"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (game.isInstalled) NeonGreen else CyberAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (!game.isInstalled) {
                        OutlinedButton(
                            onClick = {
                                onSelect()
                                onInstall()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Install",
                                modifier = Modifier.size(14.dp),
                                tint = ElectricBlue
                            )
                            Text(" Play Store", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectedGameRow(
    game: GameEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detected_game_${game.packageName}"),
        accentColor = if (isSelected) NeonGreen else GlassBorderSubtle,
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DrawableIconBox(game.appIcon, game.displayName)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${game.packageName} • v${game.versionName ?: "?"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = NeonGreen
                )
            }
        }
    }
}

@Composable
private fun DrawableIconBox(drawable: Drawable?, contentDescription: String) {
    val bitmap = remember(drawable) {
        drawable?.let {
            val w = it.intrinsicWidth.coerceAtLeast(48).coerceAtMost(128)
            val h = it.intrinsicHeight.coerceAtLeast(48).coerceAtMost(128)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bmp.asImageBitmap()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(CarbonCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Gamepad,
                contentDescription = contentDescription,
                tint = ElectricBlue
            )
        }
    }
}
