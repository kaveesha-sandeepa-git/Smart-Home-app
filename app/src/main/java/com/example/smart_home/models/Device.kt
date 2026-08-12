package com.example.smart_home.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
open class Device(
    @PrimaryKey
    var deviceId: String = "",
    var name: String = "",
    var type: String = "", // OUTLET, LIGHT, IRON, CAMERA, MULTI_SWITCH
    var status: String = "", // ON, OFF, ERROR, DISCONNECTED
    var floorId: String = "",
    var gridX: Int = 0,
    var gridY: Int = 0,
    var lastUpdated: Long = System.currentTimeMillis(),
    var totalOnTime: Long = 0, // in milliseconds
    var sessionStartTime: Long = 0 // when device turned ON
) {
    fun isOn(): Boolean = "ON" == status

    fun toggleStatus() {
        status = when (status) {
            "ON" -> "OFF"
            "OFF" -> "ON"
            else -> status
        }
    }

    override fun toString(): String {
        return "Device(deviceId='$deviceId', name='$name', type='$type', status='$status')"
    }
}
