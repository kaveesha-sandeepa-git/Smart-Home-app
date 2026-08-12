package com.example.smart_home.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.smart_home.models.Device

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices")
    fun getAllDevices(): LiveData<List<Device>>

    @Query("SELECT * FROM devices WHERE floorId = :floorId")
    fun getDevicesByFloor(floorId: String): LiveData<List<Device>>

    @Query("SELECT * FROM devices WHERE deviceId = :deviceId")
    fun getDeviceById(deviceId: String): LiveData<Device>

    @Query("SELECT * FROM devices WHERE status = 'ON'")
    fun getActiveDevices(): LiveData<List<Device>>

    @Query("SELECT COUNT(*) FROM devices WHERE status = 'ON'")
    fun getActiveDevicesCount(): LiveData<Int>

    @Query("SELECT * FROM devices WHERE type = :type")
    fun getDevicesByType(type: String): LiveData<List<Device>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDevice(device: Device)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(devices: List<Device>)

    @Update
    fun updateDevice(device: Device)

    @Query("UPDATE devices SET status = :status, lastUpdated = :timestamp WHERE deviceId = :deviceId")
    fun updateDeviceStatus(deviceId: String, status: String, timestamp: Long)

    @Query("UPDATE devices SET totalOnTime = :totalOnTime WHERE deviceId = :deviceId")
    fun updateDeviceOnTime(deviceId: String, totalOnTime: Long)

    @Delete
    fun deleteDevice(device: Device)

    @Query("DELETE FROM devices WHERE floorId = :floorId")
    fun deleteDevicesByFloor(floorId: String)

    @Query("DELETE FROM devices")
    fun deleteAllDevices()
}
