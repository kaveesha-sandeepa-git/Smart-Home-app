package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.smart_home.models.Device
import com.example.smart_home.repository.SmartHomeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for Device Control screen
 * Handles all operations for controlling individual devices
 */
class DeviceControlViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance(application)
    private val deviceIdToLoad = MutableLiveData<String>()

    // Automatically load device when ID changes
    val currentDevice: LiveData<Device> = deviceIdToLoad.switchMap { deviceId ->
        repository.getDeviceById(deviceId)
    }
    
    private var refreshJob: Job? = null

    init {
        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(1.seconds)
                // Just trigger a change notification for the existing LiveData
                // This will force observers to re-bind, updating the ticking time
                deviceIdToLoad.value = deviceIdToLoad.value
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

    // ============= DEVICE CONTROL =============

    fun loadDevice(deviceId: String) {
        deviceIdToLoad.value = deviceId
    }

    fun deleteDevice() {
        deviceIdToLoad.value?.let { deviceId ->
            repository.deleteDevice(deviceId)
        }
    }

    fun turnDeviceOn() {
        currentDevice.value?.let { device ->
            repository.updateDeviceStatus(device.deviceId, "ON")
        }
    }

    fun turnDeviceOff() {
        currentDevice.value?.let { device ->
            repository.updateDeviceStatus(device.deviceId, "OFF")
        }
    }

    fun toggleDevice() {
        currentDevice.value?.let { device ->
            repository.toggleDevice(device)
        }
    }

    // ============= LIGHT-SPECIFIC =============

    fun setBrightness(brightness: Int) {
        currentDevice.value?.let { device ->
            if (device.type == "LIGHT") {
                repository.setLightBrightness(device, brightness)
            }
        }
    }

    fun setLightSchedule(onTime: String, offTime: String) {
        currentDevice.value?.let { device ->
            if (device.type == "LIGHT") {
                repository.setLightSchedule(device, onTime, offTime)
            }
        }
    }

    fun disableLightSchedule() {
        currentDevice.value?.let { device ->
            if (device.type == "LIGHT") {
                repository.disableLightSchedule(device)
            }
        }
    }

    // ============= IRON-SPECIFIC =============

    fun setIronMaxDuration(maxMinutes: Int) {
        currentDevice.value?.let { device ->
            if (device.type == "IRON") {
                repository.setIronMaxDuration(device, maxMinutes)
                repository.monitorIronDevice(device)
            }
        }
    }

    fun formatMillisToTime(millis: Long): String {
        val totalMinutes = (millis / 1000 / 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(java.util.Locale.US, "%02d:%02d", hours, minutes)
    }

    fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    // ============= MULTI-SWITCH =============

    fun toggleMultiSwitch(switchIndex: Int) {
        currentDevice.value?.let { device ->
            if (device.type == "MULTI_SWITCH") {
                repository.toggleMultiSwitch(device, switchIndex)
            }
        }
    }
}
