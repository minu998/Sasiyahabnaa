package com.example.data.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class ThermalAlertNotifier(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "thermal_alerts_channel"
        private const val NOTIFICATION_ID_OVERHEAT = 9001
    }

    private var lastAlertTimestampMs = 0L

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Thermal & Overheating Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when battery temperature exceeds your safe gaming threshold."
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun maybeNotifyOverheating(tempCelsius: Float, thresholdCelsius: Float) {
        if (!hasNotificationPermission()) return
        val now = System.currentTimeMillis()
        // Rate limit alerts to once every 3 minutes
        if (now - lastAlertTimestampMs < 180_000L) return
        lastAlertTimestampMs = now

        createChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTemp = String.format("%.1f°C", tempCelsius)
        val formattedLimit = String.format("%.1f°C", thresholdCelsius)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Thermal Alert: Device at $formattedTemp")
            .setContentText("Exceeded $formattedLimit limit. Lower Free Fire graphics or pause charging to cool down.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Battery temperature reached $formattedTemp (threshold $formattedLimit). " +
                        "Unplug charger, reduce screen brightness, or take a short cooldown break to prevent thermal throttling."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_OVERHEAT, notification)
    }
}
