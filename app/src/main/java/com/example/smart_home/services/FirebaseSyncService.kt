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
import com.google.firebase.database.*
import kotlinx.coroutines.*

class FirebaseSyncService(context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    // Paths will be user-scoped if a user is signed in (preferencesManager.userId)
    private val baseUserPath: String = if (PreferencesManager.getInstance(context).userId.isNotEmpty()) {
        "users/${PreferencesManager.getInstance(context).userId}"
    } else {
        ""
    }
    private val floorsRef: DatabaseReference = if (baseUserPath.isNotEmpty()) database.getReference("$baseUserPath/$FLOORS_PATH") else database.getReference(FLOORS_PATH)
    private val devicesRef: DatabaseReference = if (baseUserPath.isNotEmpty()) database.getReference("$baseUserPath/$DEVICES_PATH") else database.getReference(DEVICES_PATH)
    private val reportsRef: DatabaseReference = if (baseUserPath.isNotEmpty()) database.getReference("$baseUserPath/$USAGE_REPORTS_PATH") else database.getReference(USAGE_REPORTS_PATH)
    private val connectedRef: DatabaseReference = database.getReference(".info/connected")
    private val deviceDao: DeviceDao
    private val floorDao: FloorDao
    private val lightDao: com.example.smart_home.database.LightDao
    private val ironDao: com.example.smart_home.database.IronDao
    private val reportDao: DeviceUsageReportDao
    private val preferencesManager: PreferencesManager = PreferencesManager.getInstance(context)
    private val syncStatus = MutableLiveData(false)
    private val syncError = MutableLiveData<String>()
    private val lastSyncTime = MutableLiveData<Long>()
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val db = AppDatabase.getInstance(context)
        deviceDao = db.deviceDao()
        floorDao = db.floorDao()
        lightDao = db.lightDao()
        ironDao = db.ironDao()
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
        floorsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @Suppress("UNCHECKED_CAST")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val floors = mutableListOf<Floor>()
                for (snapshot in dataSnapshot.children) {
                    val floor = snapshot.getValue(Floor::class.java)
                    if (floor != null) {
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
        floorsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val floors = mutableListOf<Floor>()
                for (snapshot in dataSnapshot.children) {
                    val floor = snapshot.getValue(Floor::class.java)
                    if (floor != null) {
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
            for (floor in floors) {
                floorDao.insertFloor(floor)
            }
        }
    }

    fun addFloor(floor: Floor) {
        floor.updatedAt = System.currentTimeMillis()
        floorsRef.child(floor.floorId).setValue(floor)
            .addOnSuccessListener {
                serviceScope.launch { floorDao.insertFloor(floor) }
                Log.d(TAG, "Floor added: ${floor.name}")
            }
            .addOnFailureListener { e ->
                syncError.postValue("Failed to add floor: ${e.message}")
                Log.e(TAG, "Failed to add floor", e)
            }
    }

    fun updateFloor(floor: Floor) {
        floor.updatedAt = System.currentTimeMillis()
        floorsRef.child(floor.floorId).setValue(floor)
            .addOnSuccessListener {
                serviceScope.launch { floorDao.updateFloor(floor) }
                Log.d(TAG, "Floor updated: ${floor.name}")
            }
            .addOnFailureListener { e ->
                syncError.postValue("Failed to update floor: ${e.message}")
            }
    }

    // ============= DEVICES SYNC =============

    fun syncDevicesFromFirebase() {
        devicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val devices = mutableListOf<Device>()
                for (snapshot in dataSnapshot.children) {
                    val type = snapshot.child("type").getValue(String::class.java) ?: ""
                    when (type) {
                        "LIGHT" -> {
                            val light = snapshot.getValue(com.example.smart_home.models.Light::class.java)
                            light?.let {
                                devices.add(it)
                                // insert subtype table as well
                                serviceScope.launch { lightDao.insertLight(it) }
                            }
                        }
                        "IRON" -> {
                            val iron = snapshot.getValue(com.example.smart_home.models.Iron::class.java)
                            iron?.let {
                                devices.add(it)
                                serviceScope.launch { ironDao.insertIron(it) }
                            }
                        }
                        else -> {
                            val device = snapshot.getValue(Device::class.java)
                            if (device != null) devices.add(device)
                        }
                    }
                }
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
        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val devices = mutableListOf<Device>()
                for (snapshot in dataSnapshot.children) {
                    val type = snapshot.child("type").getValue(String::class.java) ?: ""
                    when (type) {
                        "LIGHT" -> {
                            val light = snapshot.getValue(com.example.smart_home.models.Light::class.java)
                            light?.let {
                                devices.add(it)
                                serviceScope.launch { lightDao.insertLight(it) }
                            }
                        }
                        "IRON" -> {
                            val iron = snapshot.getValue(com.example.smart_home.models.Iron::class.java)
                            iron?.let {
                                devices.add(it)
                                serviceScope.launch { ironDao.insertIron(it) }
                            }
                        }
                        else -> {
                            val device = snapshot.getValue(Device::class.java)
                            if (device != null) devices.add(device)
                        }
                    }
                }
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
        devicesRef.child(deviceId).addValueEventListener(object : ValueEventListener {
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
        reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val reports = mutableListOf<DeviceUsageReport>()
                for (snapshot in dataSnapshot.children) {
                    val report = snapshot.getValue(DeviceUsageReport::class.java)
                    if (report != null) reports.add(report)
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
        reportsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val reports = mutableListOf<DeviceUsageReport>()
                for (snapshot in dataSnapshot.children) {
                    val report = snapshot.getValue(DeviceUsageReport::class.java)
                    if (report != null) reports.add(report)
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
            for (r in reports) {
                reportDao.insertReport(r)
            }
        }
    }

    private fun insertDevicesToDatabase(devices: List<Device>) {
        serviceScope.launch {
            for (device in devices) {
                device.lastUpdated = System.currentTimeMillis()
                deviceDao.insertDevice(device)
            }
        }
    }

    fun toggleDeviceStatus(device: Device) {
        device.toggleStatus()
        device.lastUpdated = System.currentTimeMillis()
        
        // Update local database immediately for instant UI response
        serviceScope.launch { deviceDao.updateDevice(device) }
        
        // Sync to Firebase
        devicesRef.child(device.deviceId).setValue(device)
            .addOnSuccessListener {
                Log.d(TAG, "Device status synced: ${device.name} -> ${device.status}")
            }
            .addOnFailureListener { e ->
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
        
        devicesRef.child(deviceId).child("status").setValue(status)
            .addOnSuccessListener {
                Log.d(TAG, "Device status updated: $deviceId -> $status")
            }
            .addOnFailureListener { e ->
                syncError.postValue("Failed to update device status: ${e.message}")
                Log.e(TAG, "Failed to update device status", e)
            }
    }

    fun addDevice(device: Device) {
        device.lastUpdated = System.currentTimeMillis()
        devicesRef.child(device.deviceId).setValue(device)
            .addOnSuccessListener {
                serviceScope.launch { deviceDao.insertDevice(device) }
                Log.d(TAG, "Device added: ${device.name}")
            }
            .addOnFailureListener { e ->
                syncError.postValue("Failed to add device: ${e.message}")
                Log.e(TAG, "Failed to add device", e)
            }
    }

    fun updateDevice(device: Device) {
        device.lastUpdated = System.currentTimeMillis()
        devicesRef.child(device.deviceId).setValue(device)
            .addOnSuccessListener {
                serviceScope.launch { deviceDao.updateDevice(device) }
                Log.d(TAG, "Device updated: ${device.name}")
            }
            .addOnFailureListener { e ->
                syncError.postValue("Failed to update device: ${e.message}")
                Log.e(TAG, "Failed to update device", e)
            }
    }

    fun deleteDevice(deviceId: String) {
        devicesRef.child(deviceId).removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "Device deleted: $deviceId")
            }
            .addOnFailureListener { e ->
                syncError.postValue("Failed to delete device: ${e.message}")
                Log.e(TAG, "Failed to delete device", e)
            }
    }

    // ============= STATUS OBSERVERS =============

    fun getSyncStatus(): LiveData<Boolean> = syncStatus

    fun getSyncError(): LiveData<String> = syncError

    fun getLastSyncTime(): LiveData<Long> = lastSyncTime

    fun refreshSync() {
        connectedRef.addListenerForSingleValueEvent(object : ValueEventListener {
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
