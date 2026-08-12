package com.example.smart_home.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_reports")
data class DeviceUsageReport(
    @PrimaryKey
    var reportId: String = "",
    var deviceId: String = "",
    var deviceName: String = "",
    var totalOnTimeMs: Long = 0,
    var todayOnTimeMs: Long = 0,
    var dailyToggleCount: Int = 0,
    var lastToggleTime: Long = 0,
    var energyConsumedKwh: Float = 0f,
    var generatedAt: Long = System.currentTimeMillis()
) {
    fun getTotalOnTimeFormatted(): String {
        val hours = totalOnTimeMs / (1000 * 60 * 60)
        val minutes = (totalOnTimeMs / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }
}
