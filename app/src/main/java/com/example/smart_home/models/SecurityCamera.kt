package com.example.smart_home.models

import androidx.room.Entity

@Entity(tableName = "cameras")
class SecurityCamera(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0,
    var streamUrl: String = "",
    var lastSnapshotUrl: String = "",
    var recordingEnabled: Boolean = true,
    var lastRecordingTime: Long = 0
) : Device(deviceId, name, "CAMERA", status, floorId, gridX, gridY) {

    fun captureSnapshot() {
        this.lastRecordingTime = System.currentTimeMillis()
    }
}
