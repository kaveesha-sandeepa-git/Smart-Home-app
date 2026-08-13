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
import com.google.firebase.database.*
import kotlinx.coroutines.*

class FirebaseSyncService(context: Context) {

    private val database: FirebaseDatabase by lazy {
        // Fallback to project ID based URL if default instance fails
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            FirebaseDatabase.getInstance("https://smart-home-app-eaeaa-default-rtdb.firebaseio.com/")
        }
    }
    private val floorsRef: DatabaseReference by lazy { database.getReference(FLOORS_PATH) }
    private val devicesRef: DatabaseReference by lazy { database.getReference(DEVICES_PATH) }
    private val deviceDao: DeviceDao
    private val floorDao: FloorDao
    private val syncStatus = MutableLiveData(false)
    private val syncError = MutableLiveData<String>()
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val db = AppDatabase.getInstance(context)
        deviceDao = db.deviceDao()
        floorDao = db.floorDao()
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
                Log.d(TAG, "Synced ${floors.size} floors from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
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
                syncStatus.postValue(true)
            }

            override fun onCancelled(error: DatabaseError) {
                syncStatus.postValue(false)
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
                    val device = snapshot.getValue(Device::class.java)
                    if (device != null) {
                        devices.add(device)
                    }
                }
                insertDevicesToDatabase(devices)
                Log.d(TAG, "Synced ${devices.size} devices from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
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
                    val device = snapshot.getValue(Device::class.java)
                    if (device != null) {
                        devices.add(device)
                    }
                }
                insertDevicesToDatabase(devices)
                syncStatus.postValue(true)
            }

            override fun onCancelled(error: DatabaseError) {
                syncStatus.postValue(false)
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

    fun startSync() {
        syncFloorsFromFirebase()
        syncDevicesFromFirebase()
        setupFloorsListener()
        setupDevicesListener()
    }

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val FLOORS_PATH = "floors"
        private const val DEVICES_PATH = "devices"
    }
}
