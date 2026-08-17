package com.example.smart_home.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
open class Device(
    @PrimaryKey
    var deviceId: String = "",
    var name: String = "",
    var roomName: String = "",
    var type: String = "", // OUTLET, LIGHT, IRON, CAMERA, MULTI_SWITCH
    var status: String = "", // ON, OFF, ERROR, DISCONNECTED
    var floorId: String = "",
    var gridX: Int = 0,
    var gridY: Int = 0,
    var lastUpdated: Long = System.currentTimeMillis(),
    var totalOnTime: Long = 0, // in milliseconds
    var sessionStartTime: Long = 0, // when device turned ON
    
    // Light fields
    var brightness: Int = 100,
    var scheduleOnTime: Long = 0,
    var scheduleOffTime: Long = 0,
    var schedulingEnabled: Boolean = false,
    var colorTemperature: String = "NEUTRAL",
    
    // Iron fields
    var maxOnDurationMinutes: Int = 30,
    var currentSessionMinutes: Int = 0,
    var safetyAlertActive: Boolean = false,
    
    // Outlet fields
    var powerRating: Int = 1500,
    var overloadProtection: Boolean = true,
    
    // MultiSwitch fields
    var switchCount: Int = 0,
    var switches: MutableList<MultiSwitch.SwitchState> = mutableListOf(),
    
    // Camera fields
    var streamUrl: String = "",
    var lastSnapshotUrl: String = "",
    var recordingEnabled: Boolean = true,
    var lastRecordingTime: Long = 0
) {
    @androidx.room.Ignore
    @get:com.google.firebase.database.PropertyName("on")
    @set:com.google.firebase.database.PropertyName("on")
    var powerState: Boolean? = null

    @com.google.firebase.database.Exclude
    fun isDeviceOn(): Boolean = "ON" == status

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
