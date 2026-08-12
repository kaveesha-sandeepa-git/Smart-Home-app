package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.smart_home.models.Device
import com.example.smart_home.models.Floor
import com.example.smart_home.repository.SmartHomeRepository

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
        repository.getDevicesByFloor(floorId)
    }

    // ============= GETTERS =============

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()

    fun getRepositorySuccess(): LiveData<String> = repository.getRepositorySuccess()

    // ============= SETTERS & ACTIONS =============

    fun selectFloor(floorId: String) {
        selectedFloorId.value = floorId
    }

    fun toggleDevice(device: Device) {
        repository.toggleDevice(device)
    }

    fun refreshData() {
        // Trigger a refresh by re-emitting the value
        selectedFloorId.value?.let {
            selectedFloorId.value = it
        }
    }
}
