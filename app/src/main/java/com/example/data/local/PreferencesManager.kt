package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ff_pro_optimizer_prefs", Context.MODE_PRIVATE)

    var selectedGamePackage: String
        get() = prefs.getString(KEY_SELECTED_GAME_PKG, "com.dts.freefireth") ?: "com.dts.freefireth"
        set(value) = prefs.edit().putString(KEY_SELECTED_GAME_PKG, value).apply()

    var activeProfileId: Long
        get() = prefs.getLong(KEY_ACTIVE_PROFILE_ID, 1L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_PROFILE_ID, value).apply()

    var useFahrenheit: Boolean
        get() = prefs.getBoolean(KEY_USE_FAHRENHEIT, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_FAHRENHEIT, value).apply()

    var overheatingAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERHEATING_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_OVERHEATING_ALERTS, value).apply()

    var thermalThresholdCelsius: Float
        get() = prefs.getFloat(KEY_THERMAL_THRESHOLD_C, 40.0f)
        set(value) = prefs.edit().putFloat(KEY_THERMAL_THRESHOLD_C, value).apply()

    var gamingModeSustainedEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAMING_MODE_SUSTAINED, true)
        set(value) = prefs.edit().putBoolean(KEY_GAMING_MODE_SUSTAINED, value).apply()

    var gamingModeDndEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAMING_MODE_DND, false)
        set(value) = prefs.edit().putBoolean(KEY_GAMING_MODE_DND, value).apply()

    var selectedPingEndpointId: String
        get() = prefs.getString(KEY_PING_ENDPOINT_ID, "cloudflare") ?: "cloudflare"
        set(value) = prefs.edit().putString(KEY_PING_ENDPOINT_ID, value).apply()

    var safCacheTreeUri: String?
        get() = prefs.getString(KEY_SAF_TREE_URI, null)
        set(value) = prefs.edit().putString(KEY_SAF_TREE_URI, value).apply()

    var sessionStartTimestampMs: Long
        get() = prefs.getLong(KEY_SESSION_START_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_SESSION_START_MS, value).apply()

    var manualCustomPackages: Set<String>
        get() = prefs.getStringSet(KEY_MANUAL_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_MANUAL_PACKAGES, value).apply()

    companion object {
        private const val KEY_SELECTED_GAME_PKG = "selected_game_pkg"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_USE_FAHRENHEIT = "use_fahrenheit"
        private const val KEY_OVERHEATING_ALERTS = "overheating_alerts"
        private const val KEY_THERMAL_THRESHOLD_C = "thermal_threshold_c"
        private const val KEY_GAMING_MODE_SUSTAINED = "gaming_mode_sustained"
        private const val KEY_GAMING_MODE_DND = "gaming_mode_dnd"
        private const val KEY_PING_ENDPOINT_ID = "ping_endpoint_id"
        private const val KEY_SAF_TREE_URI = "saf_tree_uri"
        private const val KEY_SESSION_START_MS = "session_start_ms"
        private const val KEY_MANUAL_PACKAGES = "manual_packages"
    }
}
