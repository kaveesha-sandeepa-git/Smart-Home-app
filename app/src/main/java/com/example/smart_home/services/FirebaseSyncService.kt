package com.example.smart_home.services

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.smart_home.database.AppDatabase
import com.example.smart_home.database.DeviceDao
import com.example.smart_home.database.FloorDao
import com.example.smart_home.models.Device
import com.example.smart_home.models.Floor
import com.example.smart_home.models.DeviceUsageReport
import com.example.smart_home.database.DeviceUsageReportDao
import com.example.smart_home.utils.PreferencesManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

class FirebaseSyncService(context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val preferencesManager: PreferencesManager = PreferencesManager.getInstance(context)
    
    // Scoped references based on userId
    private val userId: String get() = preferencesManager.userId
    
    private fun getRef(path: String): DatabaseReference? {
        val uid = userId
        if (uid.isEmpty()) {
            Log.w(TAG, "Attempted to get Firebase reference for empty userId")
            return null
        }
        return database.getReference("users/$uid/$path")
    }
    
    private val floorsRef: DatabaseReference? get() = getRef(FLOORS_PATH)
    private val devicesRef: DatabaseReference? get() = getRef(DEVICES_PATH)
    private val reportsRef: DatabaseReference? get() = getRef(USAGE_REPORTS_PATH)
    
    private val connectedRef: DatabaseReference? get() {
        return try {
            database.getReference(".info/connected")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connected reference", e)
            null
        }
    }
    
    private val deviceDao: DeviceDao
    private val floorDao: FloorDao
    private val reportDao: DeviceUsageReportDao
    
    private val syncStatus = MutableLiveData(false)
    private val syncError = MutableLiveData<String>()
    private val lastSyncTime = MutableLiveData<Long>()
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val db = AppDatabase.getInstance(context)
        deviceDao = db.deviceDao()
        floorDao = db.floorDao()
        reportDao = db.usageReportDao()
        lastSyncTime.postValue(preferencesManager.lastSyncTime)
    }

    private fun recordSyncSuccess() {
        val now = System.currentTimeMillis()
        preferencesManager.lastSyncTime = now
        preferencesManager.isConnected = true
        lastSyncTime.postValue(now)
        syncStatus.postValue(true)
    }

    // ============= FLOORS SYNC =============

    fun syncFloorsFromFirebase() {
        floorsRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            @Suppress("UNCHECKED_CAST")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val floors = mutableListOf<Floor>()
                for (snapshot in dataSnapshot.children) {
                    val floor = snapshot.getValue(Floor::class.java)
                    if (floor != null) {
                        if (floor.floorId.isBlank()) floor.floorId = snapshot.key.orEmpty()
                        floors.add(floor)
                    }
                }
                insertFloorsToDatabase(floors)
                recordSyncSuccess()
                Log.d(TAG, "Synced ${floors.size} floors from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
                syncStatus.postValue(false)
                preferencesManager.isConnected = false
                syncError.postValue("Failed to sync floors: ${error.message}")
                Log.e(TAG, "Failed to sync floors", error.toException())
            }
        })
    }

    fun setupFloorsListener() {
        floorsRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val floors = mutableListOf<Floor>()
                for (snapshot in dataSnapshot.children) {
                    val floor = snapshot.getValue(Floor::class.java)
                    if (floor != null) {
                        if (floor.floorId.isBlank()) floor.floorId = snapshot.key.orEmpty()
                        floors.add(floor)
                    }
                }
                insertFloorsToDatabase(floors)
                recordSyncSuccess()
            }

            override fun onCancelled(error: DatabaseError) {
                syncStatus.postValue(false)
                preferencesManager.isConnected = false
                syncError.postValue("Floors listener failed: ${error.message}")
            }
        })
    }

    private fun insertFloorsToDatabase(floors: List<Floor>) {
        serviceScope.launch {
            com.example.smart_home.repository.SmartHomeRepository.deviceUpdateMutex.withLock {
                floorDao.replaceAll(floors)
            }
        }
    }

    fun addFloor(floor: Floor) {
        floor.updatedAt = System.currentTimeMillis()
        floorsRef?.child(floor.floorId)?.setValue(floor)
            ?.addOnSuccessListener {
                serviceScope.launch { floorDao.insertFloor(floor) }
                Log.d(TAG, "Floor added: ${floor.name}")
            }
            ?.addOnFailureListener { e ->
                syncError.postValue("Failed to add floor: ${e.message}")
                Log.e(TAG, "Failed to add floor", e)
            }
    }

    fun updateFloor(floor: Floor) {
        floor.updatedAt = System.currentTimeMillis()
        floorsRef?.child(floor.floorId)?.setValue(floor)
            ?.addOnSuccessListener {
                serviceScope.launch { floorDao.updateFloor(floor) }
                Log.d(TAG, "Floor updated: ${floor.name}")
            }
            ?.addOnFailureListener { e ->
                syncError.postValue("Failed to update floor: ${e.message}")
            }
    }

    // ============= DEVICES SYNC =============

    fun syncDevicesFromFirebase() {
        devicesRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val devices = readDevices(dataSnapshot)
                insertDevicesToDatabase(devices)
                recordSyncSuccess()
                Log.d(TAG, "Synced ${devices.size} devices from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
                syncStatus.postValue(false)
                preferencesManager.isConnected = false
                syncError.postValue("Failed to sync devices: ${error.message}")
                Log.e(TAG, "Failed to sync devices", error.toException())
            }
        })
    }

    fun setupDevicesListener() {
        devicesRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val devices = readDevices(dataSnapshot)
                insertDevicesToDatabase(devices)
                recordSyncSuccess()
            }

            override fun onCancelled(error: DatabaseError) {
                syncStatus.postValue(false)
                preferencesManager.isConnected = false
                syncError.postValue("Devices listener failed: ${error.message}")
            }
        })
    }

    fun setupDeviceStatusListener(deviceId: String) {
        devicesRef?.child(deviceId)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val device = dataSnapshot.getValue(Device::class.java)
                if (device != null) {
                    device.lastUpdated = System.currentTimeMillis()
                    serviceScope.launch { deviceDao.updateDevice(device) }
                    Log.d(TAG, "Device updated from Firebase: ${device.name} -> ${device.status}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to listen to device $deviceId", error.toException())
            }
        })
    }

    // ============= USAGE REPORTS SYNC =============

    fun syncReportsFromFirebase() {
        reportsRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val reports = mutableListOf<DeviceUsageReport>()
                for (snapshot in dataSnapshot.children) {
                    val report = snapshot.getValue(DeviceUsageReport::class.java)
                    if (report != null) {
                        if (report.reportId.isBlank()) report.reportId = snapshot.key.orEmpty()
                        reports.add(report)
                    }
                }
                insertReportsToDatabase(reports)
                Log.d(TAG, "Synced ${reports.size} usage reports from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to sync reports", error.toException())
            }
        })
    }

    fun setupReportsListener() {
        reportsRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val reports = mutableListOf<DeviceUsageReport>()
                for (snapshot in dataSnapshot.children) {
                    val report = snapshot.getValue(DeviceUsageReport::class.java)
                    if (report != null) {
                        if (report.reportId.isBlank()) report.reportId = snapshot.key.orEmpty()
                        reports.add(report)
                    }
                }
                insertReportsToDatabase(reports)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Reports listener failed", error.toException())
            }
        })
    }

    private fun insertReportsToDatabase(reports: List<DeviceUsageReport>) {
        serviceScope.launch {
            reportDao.replaceAll(reports)
        }
    }

    private fun insertDevicesToDatabase(devices: List<Device>) {
        serviceScope.launch {
            com.example.smart_home.repository.SmartHomeRepository.deviceUpdateMutex.withLock {
                deviceDao.replaceAll(devices)
            }
        }
    }

    private fun readDevices(dataSnapshot: DataSnapshot): List<Device> {
        return dataSnapshot.children.mapNotNull { snapshot ->
            try {
                val device = snapshot.getValue(Device::class.java) ?: return@mapNotNull null

                // Firebase keys are valid device identifiers too. Supporting them lets
                // the app load entries whose payload omits a duplicate deviceId field.
                if (device.deviceId.isBlank()) device.deviceId = snapshot.key.orEmpty()
                device.type = device.type.trim().uppercase()
                device.status = device.status.trim().uppercase().ifBlank { "OFF" }

                if (device.deviceId.isBlank()) {
                    Log.w(TAG, "Skipping Firebase device without an id at ${snapshot.ref}")
                    null
                } else {
                    device
                }
            } catch (e: DatabaseException) {
                Log.e(TAG, "Skipping invalid Firebase device at ${snapshot.ref}", e)
                null
            }
        }
    }

    fun toggleDeviceStatus(device: Device) {
        device.toggleStatus()
        device.lastUpdated = System.currentTimeMillis()
        
        // Update local database immediately for instant UI response
        serviceScope.launch { deviceDao.updateDevice(device) }
        
        // Sync to Firebase
        devicesRef?.child(device.deviceId)?.setValue(device)
            ?.addOnSuccessListener {
                Log.d(TAG, "Device status synced: ${device.name} -> ${device.status}")
            }
            ?.addOnFailureListener { e ->
                syncError.postValue("Failed to update device: ${e.message}")
                Log.e(TAG, "Failed to toggle device", e)
                // Revert status on failure
                device.toggleStatus()
                serviceScope.launch { deviceDao.updateDevice(device) }
            }
    }

    fun updateDeviceStatus(deviceId: String, status: String) {
        val timestamp = System.currentTimeMillis()
        serviceScope.launch { deviceDao.updateDeviceStatus(deviceId, status, timestamp) }
        
        val updates = mapOf(
            "status" to status,
            "on" to ("ON" == status),
            "lastUpdated" to timestamp
        )
        devicesRef?.child(deviceId)?.updateChildren(updates)
            ?.addOnSuccessListener {
                Log.d(TAG, "Device status updated: $deviceId -> $status")
            }
            ?.addOnFailureListener { e ->
                syncError.postValue("Failed to update device status: ${e.message}")
                Log.e(TAG, "Failed to update device status", e)
            }
    }

    suspend fun addDevice(device: Device) = withContext(Dispatchers.IO) {
        device.lastUpdated = System.currentTimeMillis()
        try {
            val task = devicesRef?.child(device.deviceId)?.setValue(device)
            if (task != null) {
                Tasks.await(task, 5, TimeUnit.SECONDS)
                // Update local DB after successful Firebase sync
                deviceDao.insertDevice(device)
                Log.d(TAG, "Device added to Firebase and local DB: ${device.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add device to Firebase", e)
            syncError.postValue("Failed to add device: ${e.message}")
        }
    }

    suspend fun updateDevice(device: Device) = withContext(Dispatchers.IO) {
        device.lastUpdated = System.currentTimeMillis()
        try {
            val task = devicesRef?.child(device.deviceId)?.setValue(device)
            if (task != null) {
                Tasks.await(task, 5, TimeUnit.SECONDS)
                // Update local DB after successful Firebase sync
                deviceDao.updateDevice(device)
                Log.d(TAG, "Device updated in Firebase and local DB: ${device.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update device in Firebase", e)
            syncError.postValue("Sync failed: ${e.message}")
        }
    }

    suspend fun deleteDevice(deviceId: String) = withContext(Dispatchers.IO) {
        try {
            val task = devicesRef?.child(deviceId)?.removeValue()
            if (task != null) {
                Tasks.await(task, 5, TimeUnit.SECONDS)
                deviceDao.deleteDeviceById(deviceId)
                Log.d(TAG, "Device deleted from Firebase and local DB: $deviceId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete device from Firebase", e)
        }
    }

    // ============= STATUS OBSERVERS =============

    fun getSyncStatus(): LiveData<Boolean> = syncStatus

    fun getSyncError(): LiveData<String> = syncError

    fun getLastSyncTime(): LiveData<Long> = lastSyncTime

    fun refreshSync() {
        connectedRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                syncStatus.postValue(connected)
                preferencesManager.isConnected = connected

                if (connected) {
                    syncFloorsFromFirebase()
                    syncDevicesFromFirebase()
                    syncReportsFromFirebase()
                    Log.d(TAG, "Firebase connectivity refresh: connected")
                } else {
                    Log.w(TAG, "Firebase connectivity refresh: disconnected")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                syncStatus.postValue(false)
                preferencesManager.isConnected = false
                syncError.postValue("Connectivity check failed: ${error.message}")
                Log.e(TAG, "Connectivity check failed", error.toException())
            }
        })
    }

    fun startSync() {
        syncFloorsFromFirebase()
        syncDevicesFromFirebase()
        syncReportsFromFirebase()
        setupFloorsListener()
        setupDevicesListener()
        setupReportsListener()
    }

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val FLOORS_PATH = "floors"
        private const val DEVICES_PATH = "devices"
        private const val USAGE_REPORTS_PATH = "usage_reports"
    }
}
