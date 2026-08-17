package com.example.smart_home.models

class MultiSwitch(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0
) : Device(
    deviceId = deviceId,
    name = name,
    type = "MULTI_SWITCH",
    status = status,
    floorId = floorId,
    gridX = gridX,
    gridY = gridY
) {

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
