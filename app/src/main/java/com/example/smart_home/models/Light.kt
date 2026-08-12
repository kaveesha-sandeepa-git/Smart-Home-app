package com.example.smart_home.models

import androidx.room.Entity

@Entity(tableName = "lights")
class Light(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0,
    var brightness: Int = 100, // 0-100
    var scheduleOnTime: Long = 0, // HH:mm in milliseconds from midnight
    var scheduleOffTime: Long = 0,
    var schedulingEnabled: Boolean = false,
    var colorTemperature: String = "NEUTRAL" // WARM, NEUTRAL, COOL
) : Device(deviceId, name, "LIGHT", status, floorId, gridX, gridY) {

    fun setBrightnessLevel(level: Int) {
        this.brightness = level.coerceIn(0, 100)
    }
}
