package com.example.smart_home.models

import androidx.room.Entity

@Entity(tableName = "irons")
class Iron(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0,
    var maxOnDurationMinutes: Int = 30, // Maximum time iron can stay ON
    var currentSessionMinutes: Int = 0, // Current session duration
    var safetyAlertActive: Boolean = false
) : Device(deviceId, name, "IRON", status, floorId, gridX, gridY) {

    fun isTimeExceeded(): Boolean = currentSessionMinutes >= maxOnDurationMinutes

    fun checkSafety() {
        if (isTimeExceeded()) {
            this.status = "OFF"
            this.safetyAlertActive = true
        }
    }
}
