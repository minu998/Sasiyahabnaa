package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BoostStep
import com.example.data.model.BoostStepStatus
import com.example.data.model.OptimizationSummary
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardElevated
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThermalCrimson
import com.example.util.Formatters

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricBlue,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassBrush = Brush.linearGradient(
        colors = listOf(
            CarbonCardElevated.copy(alpha = 0.88f),
            CarbonCard.copy(alpha = 0.78f),
            ObsidianSurface.copy(alpha = 0.92f)
        )
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.42f),
            GlassBorderSubtle.copy(alpha = 0.45f),
            NeonGreen.copy(alpha = 0.22f)
        )
    )

    val clickModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier.clip(shape)
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .background(brush = glassBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun NeonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradientColors: List<Color> = listOf(NeonGreen, ElectricBlue)
) {
    val shape = RoundedCornerShape(16.dp)
    val activeBrush = if (enabled) {
        Brush.horizontalGradient(colors = gradientColors)
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                TextMuted.copy(alpha = 0.3f),
                TextMuted.copy(alpha = 0.25f)
            )
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = ObsidianBlack,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = TextSecondary
        ),
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(brush = activeBrush, shape = shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ObsidianBlack,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (enabled) ObsidianBlack else TextSecondary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PerformanceArcGauge(
    score: Int,
    statusLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 152.dp
) {
    val animatedScore by animateIntAsState(
        targetValue = score.coerceIn(0, 100),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "score_int"
    )
    val sweepFraction by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 220f),
        label = "score_arc"
    )

    val gaugeColor = when {
        score >= 80 -> NeonGreen
        score >= 60 -> ElectricBlue
        score >= 45 -> CyberAmber
        else -> ThermalCrimson
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12.dp.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Track arc
            drawArc(
                color = GlassBorderSubtle.copy(alpha = 0.4f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active neon arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ElectricBlue, NeonGreen, gaugeColor, ElectricBlue)
                ),
                startAngle = 140f,
                sweepAngle = 260f * sweepFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedScore",
                style = MaterialTheme.typography.displayLarge,
                color = gaugeColor
            )
            Text(
                text = "SCORE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TelemetryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    progressFraction: Float? = null,
    testTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    GlassCard(
        modifier = cardModifier,
        accentColor = accentColor,
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(accentColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (progressFraction != null) {
            Spacer(modifier = Modifier.height(10.dp))
            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction.coerceIn(0f, 1f),
                animationSpec = tween(450),
                label = "metric_progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(GlassBorderSubtle.copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor, NeonGreen)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun NeonSparklineChart(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    minRange: Float? = null,
    maxRange: Float? = null
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minVal = minRange ?: (values.minOrNull() ?: 0f)
        val maxVal = (maxRange ?: (values.maxOrNull() ?: 1f)).coerceAtLeast(minVal + 1f)
        val span = (maxVal - minVal).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()
        values.forEachIndexed { index, v ->
            val x = index * stepX
            val normalized = ((v - minVal) / span).coerceIn(0f, 1f)
            val y = size.height - (normalized * (size.height - 8.dp.toPx())) - 4.dp.toPx()
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == values.lastIndex) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ThermalWarningBanner(
    visible: Boolean,
    temperatureText: String,
    thermalStatusLabel: String,
    onOpenThermal: () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("thermal_warning_banner"),
            accentColor = ThermalCrimson,
            cornerRadius = 16.dp,
            onClick = onOpenThermal
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ThermalCrimson.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Overheating warning",
                        tint = ThermalCrimson
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "THERMAL ALERT • $temperatureText",
                        style = MaterialTheme.typography.labelLarge,
                        color = ThermalCrimson
                    )
                    Text(
                        text = "$thermalStatusLabel — Tap for cooling guide & graphics tips",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun OneTapBoostDialog(
    steps: List<BoostStep>,
    isRunning: Boolean,
    summary: OptimizationSummary?,
    useFahrenheit: Boolean,
    onDismiss: () -> Unit,
    onLaunchGame: () -> Unit,
    onInstallGame: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = NeonGreen
                        )
                        Text(
                            text = if (isRunning) "OPTIMIZING DEVICE..." else "OPTIMIZATION SUMMARY",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                    if (!isRunning) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("boost_dialog_close")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close summary",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                steps.forEach { step ->
                    val stepColor = when (step.status) {
                        BoostStepStatus.COMPLETED -> NeonGreen
                        BoostStepStatus.RUNNING -> ElectricBlue
                        BoostStepStatus.WARNING -> CyberAmber
                        BoostStepStatus.PENDING -> TextMuted
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonCard.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (step.status) {
                            BoostStepStatus.RUNNING -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ElectricBlue,
                                strokeWidth = 2.dp
                            )
                            BoostStepStatus.COMPLETED -> Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Completed",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            BoostStepStatus.WARNING -> Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Warning",
                                tint = CyberAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            BoostStepStatus.PENDING -> Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(1.5.dp, TextMuted, CircleShape)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${step.stepNumber}. ${step.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = stepColor
                            )
                            Text(
                                text = step.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (summary != null) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = NeonGreen,
                        cornerRadius = 16.dp
                    ) {
                        Text(
                            text = "PRE-LAUNCH TELEMETRY (${summary.profileName})",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryStat("Score", "${summary.scoreBefore} → ${summary.scoreAfter}")
                            SummaryStat("Free RAM", "${summary.availableRamMb} MB")
                            SummaryStat("Temp", Formatters.formatTemperature(summary.batteryTempC, useFahrenheit))
                            SummaryStat("Ping", summary.pingMs?.let { "${it}ms" } ?: "--")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        summary.recommendations.forEach { rec ->
                            Text(
                                text = "• $rec",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    if (summary.targetGame.isInstalled) {
                        NeonGradientButton(
                            text = "LAUNCH ${summary.targetGame.displayName.uppercase()}",
                            onClick = onLaunchGame,
                            icon = Icons.Rounded.PlayArrow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("launch_game_after_boost_button")
                        )
                    } else {
                        NeonGradientButton(
                            text = "INSTALL ${summary.targetGame.displayName.uppercase()}",
                            onClick = onInstallGame,
                            icon = Icons.Rounded.PlayArrow,
                            gradientColors = listOf(CyberAmber, ElectricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("install_game_after_boost_button")
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("close_boost_summary_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorderSubtle)
                    ) {
                        Text("Return to Dashboard", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}
