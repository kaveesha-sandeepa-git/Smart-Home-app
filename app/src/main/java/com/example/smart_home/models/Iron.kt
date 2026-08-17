package com.example.smart_home.models

class Iron(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0
) : Device(
    deviceId = deviceId,
    name = name,
    type = "IRON",
    status = status,
    floorId = floorId,
    gridX = gridX,
    gridY = gridY
) {

    fun isTimeExceeded(): Boolean = currentSessionMinutes >= maxOnDurationMinutes

    fun checkSafety() {
        if (isTimeExceeded()) {
            this.status = "OFF"
            this.safetyAlertActive = true
        }
    }
}
