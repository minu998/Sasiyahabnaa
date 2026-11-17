package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatTemperature(celsius: Float, useFahrenheit: Boolean): String {
        return if (useFahrenheit) {
            val f = (celsius * 9f / 5f) + 32f
            String.format(Locale.US, "%.1f°F", f)
        } else {
            String.format(Locale.US, "%.1f°C", celsius)
        }
    }

    fun formatDurationHms(elapsedMs: Long): String {
        val totalSec = (elapsedMs / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatTimestamp(timestampMs: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatBandwidthKbps(kbps: Int): String {
        if (kbps <= 0) return "--"
        return if (kbps >= 1000) {
            "${(kbps / 1000.0).roundToInt()} Mbps"
        } else {
            "$kbps Kbps"
        }
    }
}
