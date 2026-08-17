package com.example.smart_home.database

import androidx.room.*
import com.example.smart_home.models.Light

@Dao
interface LightDao {

    @Query("SELECT * FROM lights WHERE deviceId = :deviceId")
    fun getLightById(deviceId: String): Light?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLight(light: Light)

    @Update
    fun updateLight(light: Light)

    @Query("DELETE FROM lights WHERE deviceId = :deviceId")
    fun deleteLightById(deviceId: String)
}
