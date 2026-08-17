package com.example.smart_home.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_usage_sessions")
data class DeviceUsageSession(
    @PrimaryKey
    var sessionId: String = "",
    var deviceId: String = "",
    var startTime: Long = 0,
    var endTime: Long = 0,
    var durationMs: Long = 0
)
