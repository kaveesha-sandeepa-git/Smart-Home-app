package com.example.smart_home.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.smart_home.database.AppDatabase
import com.example.smart_home.database.DeviceDao
import com.example.smart_home.database.DeviceUsageReportDao
import com.example.smart_home.database.FloorDao
import com.example.smart_home.models.Device
import com.example.smart_home.models.DeviceUsageReport
import com.example.smart_home.models.Floor
import com.example.smart_home.models.Iron
import com.example.smart_home.models.Light
import com.example.smart_home.services.FirebaseSyncService
import com.example.smart_home.services.SafetyRulesService
import com.example.smart_home.services.SchedulingService
import kotlinx.coroutines.*

/**
 * Repository acts as a single source of truth for all data operations.
 * It abstracts the data layer and provides clean APIs to the UI layer.
 */
class SmartHomeRepository private constructor(context: Context) {

    private val deviceDao: DeviceDao
    private val floorDao: FloorDao
    private val reportDao: DeviceUsageReportDao
    private val firebaseService: FirebaseSyncService
    private val safetyService: SafetyRulesService
    private val schedulingService: SchedulingService

    private val repositoryError = MutableLiveData<String>()
    private val repositorySuccess = MutableLiveData<String>()
    
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val db = AppDatabase.getInstance(context)
        deviceDao = db.deviceDao()
        floorDao = db.floorDao()
        reportDao = db.usageReportDao()

        firebaseService = FirebaseSyncService(context)
        safetyService = SafetyRulesService(context, firebaseService)
        schedulingService = SchedulingService(context, firebaseService)

        initializeServices()
    }

    private fun initializeServices() {
        firebaseService.startSync()
        safetyService.startIronSafetyMonitoring()
        schedulingService.startScheduleMonitoring()

        Log.d(TAG, "SmartHomeRepository initialized with all services")
    }

    // ============= FLOOR OPERATIONS =============

    fun getAllFloors(): LiveData<List<Floor>> = floorDao.getAllFloors()

    fun getFloorById(floorId: String): LiveData<Floor> = floorDao.getFloorById(floorId)

    fun getFloorsCount(): LiveData<Int> = floorDao.getFloorsCount()

    fun addFloor(floor: Floor) {
        repositoryScope.launch {
            try {
                floorDao.insertFloor(floor)
                firebaseService.addFloor(floor)
                repositorySuccess.postValue("Floor added successfully: ${floor.name}")
                Log.d(TAG, "Floor added: ${floor.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error adding floor: ${e.message}")
                Log.e(TAG, "Error adding floor", e)
            }
        }
    }

    fun updateFloor(floor: Floor) {
        repositoryScope.launch {
            try {
                floorDao.updateFloor(floor)
                firebaseService.updateFloor(floor)
                repositorySuccess.postValue("Floor updated: ${floor.name}")
                Log.d(TAG, "Floor updated: ${floor.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error updating floor: ${e.message}")
                Log.e(TAG, "Error updating floor", e)
            }
        }
    }

    fun deleteFloor(floorId: String) {
        repositoryScope.launch {
            try {
                deviceDao.deleteDevicesByFloor(floorId)
                floorDao.deleteFloor(Floor(floorId = floorId))
                repositorySuccess.postValue("Floor deleted")
                Log.d(TAG, "Floor deleted: $floorId")
            } catch (e: Exception) {
                repositoryError.postValue("Error deleting floor: ${e.message}")
                Log.e(TAG, "Error deleting floor", e)
            }
        }
    }

    // ============= DEVICE OPERATIONS =============

    fun getAllDevices(): LiveData<List<Device>> = deviceDao.getAllDevices()

    fun getDevicesByFloor(floorId: String): LiveData<List<Device>> = deviceDao.getDevicesByFloor(floorId)

    fun getDeviceById(deviceId: String): LiveData<Device> = deviceDao.getDeviceById(deviceId)

    fun getActiveDevices(): LiveData<List<Device>> = deviceDao.getActiveDevices()

    fun getActiveDevicesCount(): LiveData<Int> = deviceDao.getActiveDevicesCount()

    fun getDevicesByType(type: String): LiveData<List<Device>> = deviceDao.getDevicesByType(type)

    fun addDevice(device: Device) {
        repositoryScope.launch {
            try {
                deviceDao.insertDevice(device)
                firebaseService.addDevice(device)
                repositorySuccess.postValue("Device added: ${device.name}")
                Log.d(TAG, "Device added: ${device.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error adding device: ${e.message}")
                Log.e(TAG, "Error adding device", e)
            }
        }
    }

    fun toggleDevice(device: Device) {
        repositoryScope.launch {
            try {
                firebaseService.toggleDeviceStatus(device)
                
                // Track usage
                if ("ON" == device.status) {
                    device.sessionStartTime = System.currentTimeMillis()
                } else {
                    if (device.sessionStartTime > 0) {
                        val sessionDuration = System.currentTimeMillis() - device.sessionStartTime
                        device.totalOnTime += sessionDuration
                        device.sessionStartTime = 0
                    }
                }

                deviceDao.updateDevice(device)
                Log.d(TAG, "Device toggled: ${device.name} -> ${device.status}")
            } catch (e: Exception) {
                repositoryError.postValue("Error toggling device: ${e.message}")
                Log.e(TAG, "Error toggling device", e)
            }
        }
    }

    fun updateDeviceStatus(deviceId: String, status: String) {
        repositoryScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                deviceDao.updateDeviceStatus(deviceId, status, timestamp)
                firebaseService.updateDeviceStatus(deviceId, status)
                Log.d(TAG, "Device status updated: $deviceId -> $status")
            } catch (e: Exception) {
                repositoryError.postValue("Error updating device status: ${e.message}")
                Log.e(TAG, "Error updating device status", e)
            }
        }
    }

    fun updateDevice(device: Device) {
        repositoryScope.launch {
            try {
                device.lastUpdated = System.currentTimeMillis()
                deviceDao.updateDevice(device)
                firebaseService.updateDevice(device)
                repositorySuccess.postValue("Device updated: ${device.name}")
                Log.d(TAG, "Device updated: ${device.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error updating device: ${e.message}")
                Log.e(TAG, "Error updating device", e)
            }
        }
    }

    fun deleteDevice(deviceId: String) {
        repositoryScope.launch {
            try {
                firebaseService.deleteDevice(deviceId)
                repositorySuccess.postValue("Device deleted")
                Log.d(TAG, "Device deleted: $deviceId")
            } catch (e: Exception) {
                repositoryError.postValue("Error deleting device: ${e.message}")
                Log.e(TAG, "Error deleting device", e)
            }
        }
    }

    // ============= LIGHT-SPECIFIC OPERATIONS =============

    fun setLightSchedule(light: Light, onTime: String, offTime: String) {
        repositoryScope.launch {
            try {
                schedulingService.setLightSchedule(light, onTime, offTime)
                deviceDao.updateDevice(light)
                firebaseService.updateDevice(light)
                repositorySuccess.postValue("Schedule set for ${light.name}")
                Log.d(TAG, "Light schedule set: ${light.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error setting schedule: ${e.message}")
                Log.e(TAG, "Error setting schedule", e)
            }
        }
    }

    fun disableLightSchedule(light: Light) {
        repositoryScope.launch {
            try {
                schedulingService.disableSchedule(light)
                deviceDao.updateDevice(light)
                firebaseService.updateDevice(light)
                repositorySuccess.postValue("Schedule disabled for ${light.name}")
                Log.d(TAG, "Light schedule disabled: ${light.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error disabling schedule: ${e.message}")
                Log.e(TAG, "Error disabling schedule", e)
            }
        }
    }

    fun setLightBrightness(light: Light, brightness: Int) {
        repositoryScope.launch {
            try {
                light.brightness = brightness
                deviceDao.updateDevice(light)
                firebaseService.updateDevice(light)
                Log.d(TAG, "Light brightness set: ${light.name} -> $brightness%")
            } catch (e: Exception) {
                repositoryError.postValue("Error setting brightness: ${e.message}")
                Log.e(TAG, "Error setting brightness", e)
            }
        }
    }

    // ============= IRON-SPECIFIC OPERATIONS =============

    fun setIronMaxDuration(iron: Iron, maxMinutes: Int) {
        repositoryScope.launch {
            try {
                iron.maxOnDurationMinutes = maxMinutes
                deviceDao.updateDevice(iron)
                firebaseService.updateDevice(iron)
                safetyService.setMaxDuration(iron.deviceId, maxMinutes)
                repositorySuccess.postValue("Max duration set for ${iron.name}")
                Log.d(TAG, "Iron max duration set: ${iron.name} -> $maxMinutes min")
            } catch (e: Exception) {
                repositoryError.postValue("Error setting max duration: ${e.message}")
                Log.e(TAG, "Error setting max duration", e)
            }
        }
    }

    fun monitorIronDevice(iron: Iron) {
        safetyService.monitorIronDevice(iron)
    }

    // ============= USAGE REPORTING =============

    fun getAllReports(): LiveData<List<DeviceUsageReport>> = reportDao.getAllReports()

    fun getReportsByDevice(deviceId: String): LiveData<List<DeviceUsageReport>> = reportDao.getReportsByDevice(deviceId)

    fun getReportsByDateRange(startTime: Long, endTime: Long): LiveData<List<DeviceUsageReport>> = 
        reportDao.getReportsByDateRange(startTime, endTime)

    fun generateDeviceReport(device: Device) {
        repositoryScope.launch {
            try {
                val report = DeviceUsageReport(
                    reportId = "report_${device.deviceId}_${System.currentTimeMillis()}",
                    deviceId = device.deviceId,
                    deviceName = device.name,
                    totalOnTimeMs = device.totalOnTime,
                    energyConsumedKwh = calculateEnergyConsumption(device)
                )

                reportDao.insertReport(report)
                firebaseService.addDevice(device) // Sync to Firebase

                repositorySuccess.postValue("Report generated for ${device.name}")
                Log.d(TAG, "Report generated: ${device.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error generating report: ${e.message}")
                Log.e(TAG, "Error generating report", e)
            }
        }
    }

    private fun calculateEnergyConsumption(device: Device): Float {
        // Simplified calculation: (hours * watts) / 1000 = kWh
        // Assuming different power ratings per device type
        var powerRating = 1500f // Default wattage

        when (device.type) {
            "LIGHT" -> powerRating = 60f
            "CAMERA" -> powerRating = 5f
            "IRON" -> powerRating = 1500f
            "OUTLET" -> powerRating = 1500f
        }

        val hours = device.totalOnTime / (1000f * 60 * 60)
        return (hours * powerRating) / 1000.0f
    }

    // ============= OBSERVERS =============

    fun getRepositoryError(): LiveData<String> = repositoryError

    fun getRepositorySuccess(): LiveData<String> = repositorySuccess

    fun getSyncStatus(): LiveData<Boolean> = firebaseService.getSyncStatus()

    fun getLastSyncTime(): LiveData<Long> = firebaseService.getLastSyncTime()

    fun refreshFirebaseSync() = firebaseService.refreshSync()

    fun getSafetyAlerts(): LiveData<String?> = safetyService.getSafetyAlerts()

    fun getScheduleEvents(): LiveData<String?> = schedulingService.getScheduleEvents()

    // ============= CLEANUP =============

    fun shutdown() {
        safetyService.stopIronSafetyMonitoring()
        schedulingService.stopScheduleMonitoring()
        repositoryScope.cancel()
        Log.d(TAG, "SmartHomeRepository shutdown")
    }

    companion object {
        private const val TAG = "SmartHomeRepository"
        
        @Volatile
        private var INSTANCE: SmartHomeRepository? = null

        @JvmStatic
        fun getInstance(context: Context): SmartHomeRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SmartHomeRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
