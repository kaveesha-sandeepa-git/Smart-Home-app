package com.example.smart_home.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_reports")
data class DeviceUsageReport(
    @PrimaryKey
    var reportId: String = "",
    var deviceId: String = "",
    var deviceName: String = "",
    var roomName: String = "", // New field for room identification in reports
    var floorName: String = "", // New field for floor identification in reports
    var totalOnTimeMs: Long = 0,
    var todayOnTimeMs: Long = 0,
    var dailyToggleCount: Int = 0,
    var lastToggleTime: Long = 0,
    var energyConsumedKwh: Float = 0f,
    var status: String = "OFF", // New field for real-time status on report tab
    var generatedAt: Long = System.currentTimeMillis()
) {
    fun getTotalOnTimeFormatted(): String {
        val seconds = (totalOnTimeMs / 1000) % 60
        val minutes = (totalOnTimeMs / (1000 * 60)) % 60
        val hours = totalOnTimeMs / (1000 * 60 * 60)
        
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }
}
