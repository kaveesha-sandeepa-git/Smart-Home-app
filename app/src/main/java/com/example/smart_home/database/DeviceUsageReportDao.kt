package com.example.smart_home.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.smart_home.models.DeviceUsageReport

@Dao
interface DeviceUsageReportDao {

    @Query("SELECT * FROM usage_reports ORDER BY generatedAt DESC")
    fun getAllReports(): LiveData<List<DeviceUsageReport>>

    @Query("SELECT * FROM usage_reports WHERE deviceId = :deviceId ORDER BY generatedAt DESC")
    fun getReportsByDevice(deviceId: String): LiveData<List<DeviceUsageReport>>

    @Query("SELECT * FROM usage_reports WHERE generatedAt >= :startTime AND generatedAt <= :endTime ORDER BY generatedAt DESC")
    fun getReportsByDateRange(startTime: Long, endTime: Long): LiveData<List<DeviceUsageReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReport(report: DeviceUsageReport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(reports: List<DeviceUsageReport>)

    @Transaction
    fun replaceAll(reports: List<DeviceUsageReport>) {
        deleteAllReports()
        insertAll(reports)
    }

    @Query("DELETE FROM usage_reports WHERE generatedAt < :cutoffTime")
    fun deleteOldReports(cutoffTime: Long)

    @Query("DELETE FROM usage_reports")
    fun deleteAllReports()
}
