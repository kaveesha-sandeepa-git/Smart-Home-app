package com.example.smart_home.models

class Light(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0
) : Device(
    deviceId = deviceId,
    name = name,
    type = "LIGHT",
    status = status,
    floorId = floorId,
    gridX = gridX,
    gridY = gridY
) {

    fun setBrightnessLevel(level: Int) {
        this.brightness = level.coerceIn(0, 100)
    }
}
