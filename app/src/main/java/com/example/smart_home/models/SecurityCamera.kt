package com.example.smart_home.models

class SecurityCamera(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0
) : Device(
    deviceId = deviceId,
    name = name,
    type = "CAMERA",
    status = status,
    floorId = floorId,
    gridX = gridX,
    gridY = gridY
) {

    fun captureSnapshot() {
        this.lastRecordingTime = System.currentTimeMillis()
    }
}
