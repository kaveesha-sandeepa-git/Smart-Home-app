package com.example.smart_home.utils

object Constants {

    // Device Types
    const val DEVICE_TYPE_OUTLET = "OUTLET"
    const val DEVICE_TYPE_LIGHT = "LIGHT"
    const val DEVICE_TYPE_IRON = "IRON"
    const val DEVICE_TYPE_CAMERA = "CAMERA"
    const val DEVICE_TYPE_MULTI_SWITCH = "MULTI_SWITCH"

    // Device Status
    const val STATUS_ON = "ON"
    const val STATUS_OFF = "OFF"
    const val STATUS_ERROR = "ERROR"
    const val STATUS_DISCONNECTED = "DISCONNECTED"

    // Iron Device Defaults
    const val IRON_DEFAULT_MAX_DURATION_MINUTES = 30
    const val IRON_SAFETY_CHECK_INTERVAL_MS = 5000

    // Light Defaults
    const val LIGHT_DEFAULT_BRIGHTNESS = 100

    // Sync Intervals
    const val SYNC_INTERVAL_MS = 5000L // 5 seconds
    const val SAFETY_CHECK_INTERVAL_MS = 5000L // 5 seconds
    const val SCHEDULE_CHECK_INTERVAL_MS = 60000L // 1 minute

    // Database
    const val DB_NAME = "smart_home_db"

    // Firebase Paths
    const val FIREBASE_FLOORS_PATH = "floors"
    const val FIREBASE_DEVICES_PATH = "devices"
    const val FIREBASE_USAGE_REPORTS_PATH = "usage_reports"

    // Preferences
    const val PREF_FILE_NAME = "smart_home_prefs"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val PREF_SAFETY_ALERTS_ENABLED = "safety_alerts_enabled"
    const val PREF_LAST_SYNC_TIME = "last_sync_time"
}
