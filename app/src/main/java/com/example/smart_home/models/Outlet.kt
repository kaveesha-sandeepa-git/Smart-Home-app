package com.example.smart_home.models

class Outlet(
    deviceId: String = "",
    name: String = "",
    status: String = "",
    floorId: String = "",
    gridX: Int = 0,
    gridY: Int = 0
) : Device(
    deviceId = deviceId,
    name = name,
    type = "OUTLET",
    status = status,
    floorId = floorId,
    gridX = gridX,
    gridY = gridY
)
