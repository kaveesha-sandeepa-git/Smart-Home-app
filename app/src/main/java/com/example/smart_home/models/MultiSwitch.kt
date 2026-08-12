package com.example.smart_home.models

import androidx.room.Entity

@Entity(tableName = "multi_switches")
class MultiSwitch(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0,
    var switchCount: Int = 0,
    var switches: MutableList<SwitchState> = mutableListOf()
) : Device(deviceId, name, "MULTI_SWITCH", status, floorId, gridX, gridY) {

    class SwitchState(
        var switchNumber: Int = 0,
        var state: String = "OFF" // ON, OFF
    ) {
        fun toggle() {
            state = if ("ON" == state) "OFF" else "ON"
        }
    }

    fun toggleSwitch(switchIndex: Int) {
        if (switchIndex in switches.indices) {
            switches[switchIndex].toggle()
        }
    }
}
