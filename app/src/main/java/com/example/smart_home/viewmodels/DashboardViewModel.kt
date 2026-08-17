package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.smart_home.models.Device
import com.example.smart_home.models.Floor
import com.example.smart_home.repository.SmartHomeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for Dashboard screen
 * Handles all UI state and data for the dashboard
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance(application)
    private val selectedFloorId = MutableLiveData<String>()
    
    val allDevices: LiveData<List<Device>> = repository.getAllDevices()
    val activeDevicesCount: LiveData<Int> = repository.getActiveDevicesCount()
    val floors: LiveData<List<Floor>> = repository.getAllFloors()

    // Switch to selected floor devices
    val currentFloorDevices: LiveData<List<Device>> = selectedFloorId.switchMap { floorId ->
        if (floorId == "all") {
            repository.getAllDevices()
        } else {
            repository.getDevicesByFloor(floorId)
        }
    }

    private var refreshJob: Job? = null

    init {
        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(5.seconds)
                selectedFloorId.value?.let {
                    selectedFloorId.value = it
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    // ============= GETTERS =============

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()

    fun getRepositorySuccess(): LiveData<String> = repository.getRepositorySuccess()

    fun isAllFloorsSelected(): Boolean = selectedFloorId.value == "all"

    fun getUniqueRoomNames(): List<String> {
        return allDevices.value?.map { it.roomName }?.filter { it.isNotBlank() }?.distinct() ?: emptyList()
    }

    // ============= SETTERS & ACTIONS =============

    fun selectFloor(floorId: String) {
        selectedFloorId.value = floorId
    }

    fun toggleDevice(device: Device) {
        repository.toggleDevice(device)
    }

    fun createNewDevice(name: String, room: String, type: String, floorId: String) {
        val deviceId = "dev_${System.currentTimeMillis()}"
        val device = Device(
            deviceId = deviceId,
            name = name,
            roomName = room,
            type = type,
            floorId = floorId,
            status = "OFF"
        )
        device.powerState = false
        
        if (type == "MULTI_SWITCH") {
            device.switchCount = 3
            device.switches = mutableListOf(
                com.example.smart_home.models.MultiSwitch.SwitchState(1, "OFF"),
                com.example.smart_home.models.MultiSwitch.SwitchState(2, "OFF"),
                com.example.smart_home.models.MultiSwitch.SwitchState(3, "OFF")
            )
        }
        
        repository.addDevice(device)
    }

    fun refreshData() {
        // Trigger a refresh by re-emitting the value
        selectedFloorId.value?.let {
            selectedFloorId.value = it
        }
    }
}
