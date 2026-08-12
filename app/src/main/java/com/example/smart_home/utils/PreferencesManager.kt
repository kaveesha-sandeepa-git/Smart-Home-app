package com.example.smart_home.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.smart_home.utils.Constants.PREF_FILE_NAME
import com.example.smart_home.utils.Constants.PREF_LAST_SYNC_TIME
import com.example.smart_home.utils.Constants.PREF_NOTIFICATIONS_ENABLED
import com.example.smart_home.utils.Constants.PREF_SAFETY_ALERTS_ENABLED

/**
 * Manager for SharedPreferences
 * Handles all app-wide settings persistence
 */
class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context)
                INSTANCE = instance
                instance
            }
        }
    }

    // ============= USER PREFERENCES =============

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, true)
        set(enabled) = prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled).apply()

    var safetyAlertsEnabled: Boolean
        get() = prefs.getBoolean(PREF_SAFETY_ALERTS_ENABLED, true)
        set(enabled) = prefs.edit().putBoolean(PREF_SAFETY_ALERTS_ENABLED, enabled).apply()

    var autoSyncEnabled: Boolean
        get() = prefs.getBoolean("auto_sync_enabled", true)
        set(enabled) = prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()

    // ============= THEME & UI PREFERENCES =============

    var darkModeEnabled: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(enabled) = prefs.edit().putBoolean("dark_mode", enabled).apply()

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(language) = prefs.edit().putString("language", language).apply()

    // ============= SYNC & CONNECTIVITY =============

    var lastSyncTime: Long
        get() = prefs.getLong(PREF_LAST_SYNC_TIME, 0)
        set(timestamp) = prefs.edit().putLong(PREF_LAST_SYNC_TIME, timestamp).apply()

    var syncInterval: Int
        get() = prefs.getInt("sync_interval", 15)
        set(intervalMinutes) = prefs.edit().putInt("sync_interval", intervalMinutes).apply()

    var isConnected: Boolean
        get() = prefs.getBoolean("is_connected", false)
        set(connected) = prefs.edit().putBoolean("is_connected", connected).apply()

    // ============= DEVICE PREFERENCES =============

    var defaultFloorId: String
        get() = prefs.getString("default_floor_id", "") ?: ""
        set(floorId) = prefs.edit().putString("default_floor_id", floorId).apply()

    var lastViewedDeviceId: String
        get() = prefs.getString("last_viewed_device", "") ?: ""
        set(deviceId) = prefs.edit().putString("last_viewed_device", deviceId).apply()

    // ============= SAFETY SETTINGS =============

    var ironMaxDurationDefault: Int
        get() = prefs.getInt("iron_max_duration", 30)
        set(minutes) = prefs.edit().putInt("iron_max_duration", minutes).apply()

    var maxPowerWarning: Int
        get() = prefs.getInt("max_power_warning", 5000)
        set(wattage) = prefs.edit().putInt("max_power_warning", wattage).apply()

    // ============= REPORTING =============

    var lastReportGeneratedTime: Long
        get() = prefs.getLong("last_report_time", 0)
        set(timestamp) = prefs.edit().putLong("last_report_time", timestamp).apply()

    var reportingEnabled: Boolean
        get() = prefs.getBoolean("reporting_enabled", true)
        set(enabled) = prefs.edit().putBoolean("reporting_enabled", enabled).apply()

    // ============= USER SESSION =============

    var userId: String
        get() = prefs.getString("user_id", "") ?: ""
        set(userId) = prefs.edit().putString("user_id", userId).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(email) = prefs.edit().putString("user_email", email).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("first_launch", true)
        set(first) = prefs.edit().putBoolean("first_launch", first).apply()

    // ============= UTILITY METHODS =============

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }

    fun clearUserData() {
        prefs.edit()
            .remove("user_id")
            .remove("user_email")
            .remove("default_floor_id")
            .apply()
    }

    var totalAppOpenTime: Long
        get() = prefs.getLong("total_open_time", 0)
        set(time) = prefs.edit().putLong("total_open_time", time).apply()
}
