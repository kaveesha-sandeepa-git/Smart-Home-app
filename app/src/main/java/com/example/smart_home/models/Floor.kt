package com.example.smart_home.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "floors")
class Floor(
    @PrimaryKey
    var floorId: String = "",
    var name: String = "",
    var description: String = "",
    var imageUrl: String = "", // Floor plan image
    var gridWidth: Int = 0, // Number of grid cells horizontally
    var gridHeight: Int = 0, // Number of grid cells vertically
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    override fun toString(): String {
        return "Floor(floorId='$floorId', name='$name', gridWidth=$gridWidth, gridHeight=$gridHeight)"
    }
}
