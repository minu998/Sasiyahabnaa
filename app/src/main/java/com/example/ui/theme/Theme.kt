package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GamingDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = ObsidianBlack,
    primaryContainer = NeonGreenDim.copy(alpha = 0.18f),
    onPrimaryContainer = NeonGreen,
    secondary = ElectricBlue,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElectricBlueDim.copy(alpha = 0.18f),
    onSecondaryContainer = ElectricBlue,
    tertiary = CyberAmber,
    onTertiary = ObsidianBlack,
    tertiaryContainer = CyberAmber.copy(alpha = 0.16f),
    onTertiaryContainer = CyberAmber,
    error = ThermalCrimson,
    onError = TextPrimary,
    errorContainer = ThermalCrimson.copy(alpha = 0.18f),
    onErrorContainer = ThermalCrimson,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = CarbonCard,
    surfaceContainerHigh = CarbonCardElevated,
    outline = GlassBorderSubtle,
    outlineVariant = GlassBorderSubtle.copy(alpha = 0.5f)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun FFProOptimizerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GamingDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
