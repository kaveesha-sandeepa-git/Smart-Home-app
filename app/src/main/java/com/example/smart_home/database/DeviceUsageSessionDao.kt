package com.example.smart_home.database

import androidx.room.*
import com.example.smart_home.models.DeviceUsageSession

@Dao
interface DeviceUsageSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DeviceUsageSession)

    @Query("SELECT * FROM device_usage_sessions WHERE deviceId = :deviceId")
    suspend fun getSessionsByDevice(deviceId: String): List<DeviceUsageSession>

    @Query("SELECT * FROM device_usage_sessions WHERE startTime >= :start AND startTime <= :end")
    suspend fun getSessionsByDateRange(start: Long, end: Long): List<DeviceUsageSession>

    @Query("SELECT SUM(durationMs) FROM device_usage_sessions WHERE deviceId = :deviceId AND startTime >= :start AND startTime <= :end")
    suspend fun getTotalDuration(deviceId: String, start: Long, end: Long): Long?

    @Query("DELETE FROM device_usage_sessions")
    suspend fun deleteAllSessions()
}
