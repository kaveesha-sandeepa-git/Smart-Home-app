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
import com.example.smart_home.services.FirebaseSyncService
import com.example.smart_home.services.SafetyRulesService
import com.example.smart_home.services.SchedulingService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock

/**
 * Repository acts as a single source of truth for all data operations.
 * It abstracts the data layer and provides clean APIs to the UI layer.
 */
class SmartHomeRepository private constructor(context: Context) {

    private val deviceDao: DeviceDao
    private val floorDao: FloorDao
    private val reportDao: DeviceUsageReportDao
    private val sessionDao: com.example.smart_home.database.DeviceUsageSessionDao
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
        sessionDao = db.usageSessionDao()

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
            SmartHomeRepository.deviceUpdateMutex.withLock {
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
    }

    fun updateFloor(floor: Floor) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
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
    }

    fun deleteFloor(floorId: String) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
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
    }

    // ============= DEVICE OPERATIONS =============

    fun getAllDevices(): LiveData<List<Device>> = deviceDao.getAllDevices()

    fun getDevicesByFloor(floorId: String): LiveData<List<Device>> = deviceDao.getDevicesByFloor(floorId)

    fun getDeviceById(deviceId: String): LiveData<Device> = deviceDao.getDeviceById(deviceId)

    fun getActiveDevices(): LiveData<List<Device>> = deviceDao.getActiveDevices()

    fun getActiveDevicesCount(): LiveData<Int> = deviceDao.getActiveDevicesCount()

    fun getDevicesByType(type: String): LiveData<List<Device>> = deviceDao.getDevicesByType(type)

    suspend fun getAllDevicesSync(): List<Device> = deviceDao.getAllDevicesSync()

    fun addDevice(device: Device) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
                try {
                    firebaseService.addDevice(device)
                    // We update local DB inside firebaseService.addDevice on success
                    repositorySuccess.postValue("Device added: ${device.name}")
                    Log.d(TAG, "Device added and synced: ${device.name}")
                } catch (e: Exception) {
                    repositoryError.postValue("Error adding device: ${e.message}")
                    Log.e(TAG, "Error adding device", e)
                }
            }
        }
    }

    fun toggleDevice(device: Device) {
        val newStatus = if (device.status == "ON") "OFF" else "ON"
        updateDeviceStatus(device.deviceId, newStatus)
    }

    fun updateDeviceStatus(deviceId: String, status: String) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
                try {
                    val timestamp = System.currentTimeMillis()
                    val device = deviceDao.getDeviceByIdSync(deviceId) ?: return@withLock
                    val oldStatus = device.status
                    
                    if (status == oldStatus) return@withLock // No change

                    device.status = status
                    device.powerState = ("ON" == status)
                    device.lastUpdated = timestamp
                    
                    if ("ON" == status && "ON" != oldStatus) {
                        device.sessionStartTime = timestamp
                    } else if ("OFF" == status && "OFF" != oldStatus) {
                        if (device.sessionStartTime > 0) {
                            val duration = timestamp - device.sessionStartTime
                            device.totalOnTime += duration
                            
                            val session = com.example.smart_home.models.DeviceUsageSession(
                                sessionId = "sess_${device.deviceId}_${device.sessionStartTime}",
                                deviceId = device.deviceId,
                                startTime = device.sessionStartTime,
                                endTime = timestamp,
                                durationMs = duration
                            )
                            sessionDao.insertSession(session)
                            device.sessionStartTime = 0
                        }
                    }
                    
                    // Save to local DB
                    deviceDao.updateDevice(device)
                    
                    // Sync WHOLE device object to Firebase to ensure status AND sessionStartTime are persisted
                    firebaseService.updateDevice(device)
                    
                    Log.d(TAG, "Device status updated and synced: ${device.name} -> $status")
                } catch (e: Exception) {
                    repositoryError.postValue("Error updating device status: ${e.message}")
                    Log.e(TAG, "Error updating device status", e)
                }
            }
        }
    }

    fun updateDevice(device: Device) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
                try {
                    device.powerState = ("ON" == device.status)
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
    }

    fun deleteDevice(deviceId: String) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
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
    }

    // ============= LIGHT-SPECIFIC OPERATIONS =============

    fun setLightSchedule(device: Device, onTime: String, offTime: String) {
        repositoryScope.launch {
            try {
                schedulingService.setLightSchedule(device, onTime, offTime)
                deviceDao.updateDevice(device)
                firebaseService.updateDevice(device)
                repositorySuccess.postValue("Schedule set for ${device.name}")
                Log.d(TAG, "Light schedule set: ${device.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error setting schedule: ${e.message}")
                Log.e(TAG, "Error setting schedule", e)
            }
        }
    }

    fun disableLightSchedule(device: Device) {
        repositoryScope.launch {
            try {
                schedulingService.disableSchedule(device)
                deviceDao.updateDevice(device)
                firebaseService.updateDevice(device)
                repositorySuccess.postValue("Schedule disabled for ${device.name}")
                Log.d(TAG, "Light schedule disabled: ${device.name}")
            } catch (e: Exception) {
                repositoryError.postValue("Error disabling schedule: ${e.message}")
                Log.e(TAG, "Error disabling schedule", e)
            }
        }
    }

    fun setLightBrightness(device: Device, brightness: Int) {
        repositoryScope.launch {
            try {
                device.brightness = brightness
                deviceDao.updateDevice(device)
                firebaseService.updateDevice(device)
                Log.d(TAG, "Light brightness set: ${device.name} -> $brightness%")
            } catch (e: Exception) {
                repositoryError.postValue("Error setting brightness: ${e.message}")
                Log.e(TAG, "Error setting brightness", e)
            }
        }
    }

    // ============= IRON-SPECIFIC OPERATIONS =============

    fun setIronMaxDuration(device: Device, maxMinutes: Int) {
        repositoryScope.launch {
            try {
                device.maxOnDurationMinutes = maxMinutes
                deviceDao.updateDevice(device)
                firebaseService.updateDevice(device)
                safetyService.setMaxDuration(device.deviceId, maxMinutes)
                repositorySuccess.postValue("Max duration set for ${device.name}")
                Log.d(TAG, "Iron max duration set: ${device.name} -> $maxMinutes min")
            } catch (e: Exception) {
                repositoryError.postValue("Error setting max duration: ${e.message}")
                Log.e(TAG, "Error setting max duration", e)
            }
        }
    }

    fun monitorIronDevice(device: Device) {
        safetyService.monitorIronDevice(device)
    }

    // ============= MULTI-SWITCH OPERATIONS =============

    fun toggleMultiSwitch(device: Device, switchIndex: Int) {
        repositoryScope.launch {
            try {
                if (device.type == "MULTI_SWITCH" && switchIndex in device.switches.indices) {
                    device.switches[switchIndex].toggle()
                    device.lastUpdated = System.currentTimeMillis()
                    deviceDao.updateDevice(device)
                    firebaseService.updateDevice(device)
                    Log.d(TAG, "Multi-switch toggled: ${device.name} switch $switchIndex -> ${device.switches[switchIndex].state}")
                }
            } catch (e: Exception) {
                repositoryError.postValue("Error toggling multi-switch: ${e.message}")
                Log.e(TAG, "Error toggling multi-switch", e)
            }
        }
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
                    energyConsumedKwh = calculateEnergy(device, device.totalOnTime)
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

    fun calculateEnergy(device: Device, durationMs: Long): Float {
        // Simplified calculation: (hours * watts) / 1000 = kWh
        // Assuming different power ratings per device type
        var powerRating = 1500f // Default wattage

        when (device.type) {
            "LIGHT" -> powerRating = 60f
            "CAMERA" -> powerRating = 5f
            "IRON" -> powerRating = 1500f
            "OUTLET" -> powerRating = 1500f
        }

        val hours = durationMs / (1000f * 60 * 60)
        return (hours * powerRating) / 1000.0f
    }

    fun replaceDevices(devices: List<Device>) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
                deviceDao.replaceAll(devices)
            }
        }
    }

    fun replaceFloors(floors: List<Floor>) {
        repositoryScope.launch {
            SmartHomeRepository.deviceUpdateMutex.withLock {
                floorDao.replaceAll(floors)
            }
        }
    }

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

    fun clearData() {
        repositoryScope.launch {
            deviceDao.deleteAllDevices()
            floorDao.deleteAllFloors()
            reportDao.deleteAllReports()
            sessionDao.deleteAllSessions()
            Log.d(TAG, "All local data cleared")
        }
    }

    // ============= USAGE ANALYSIS =============

    suspend fun getTotalUsageTime(deviceId: String, start: Long, end: Long): Long {
        return sessionDao.getTotalDuration(deviceId, start, end) ?: 0L
    }

    suspend fun getAllDeviceUsage(start: Long, end: Long): Map<String, Long> {
        val allDevices = deviceDao.getAllDevicesSync()
        val sessions = sessionDao.getSessionsByDateRange(start, end)
        
        val usageMap = sessions.groupBy { it.deviceId }
            .mapValues { entry -> entry.value.sumOf { it.durationMs } }
            .toMutableMap()
            
        // Add ongoing sessions
        for (device in allDevices) {
            if (device.status == "ON" && device.sessionStartTime > 0) {
                // If session started before 'end' and hasn't ended yet
                if (device.sessionStartTime < end) {
                    val sessionStart = Math.max(device.sessionStartTime, start)
                    val sessionEnd = Math.min(System.currentTimeMillis(), end)
                    if (sessionEnd > sessionStart) {
                        val ongoingDuration = sessionEnd - sessionStart
                        val currentTotal = usageMap[device.deviceId] ?: 0L
                        usageMap[device.deviceId] = currentTotal + ongoingDuration
                    }
                }
            }
        }
        
        return usageMap
    }

    companion object {
        private const val TAG = "SmartHomeRepository"
        val deviceUpdateMutex = kotlinx.coroutines.sync.Mutex()
        
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
