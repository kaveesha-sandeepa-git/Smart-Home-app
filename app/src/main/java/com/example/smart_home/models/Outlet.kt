package com.example.smart_home.models

import androidx.room.Entity

@Entity(tableName = "outlets")
class Outlet(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0,
    var powerRating: Int = 1500, // Wattage
    var overloadProtection: Boolean = true
) : Device(deviceId, name, "OUTLET", status, floorId, gridX, gridY)
