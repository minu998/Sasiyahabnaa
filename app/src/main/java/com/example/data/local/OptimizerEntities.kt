package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "optimization_history")
data class OptimizationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val gameName: String,
    val gamePackage: String,
    val profileName: String,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val ramAvailableMb: Long,
    val batteryTempCelsius: Float,
    val pingMs: Int?,
    val cacheClearedBytes: Long,
    val actionsSummary: String
)

@Entity(tableName = "optimization_profiles")
data class OptimizationProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val isBuiltIn: Boolean = false,
    val autoCleanCache: Boolean = true,
    val enableSustainedPerformance: Boolean = true,
    val enableGamingDnd: Boolean = false,
    val runNetworkPreCheck: Boolean = true,
    val thermalAlertThresholdC: Float = 40.0f,
    val recommendedGraphicsPreset: String = "Smooth / High FPS"
)
