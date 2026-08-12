package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.smart_home.models.Device
import com.example.smart_home.models.Iron
import com.example.smart_home.models.Light
import com.example.smart_home.repository.SmartHomeRepository

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

    // ============= GETTERS =============

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()

    fun getRepositorySuccess(): LiveData<String> = repository.getRepositorySuccess()

    // ============= DEVICE CONTROL =============

    fun loadDevice(deviceId: String) {
        deviceIdToLoad.value = deviceId
    }

    fun turnDeviceOn() {
        currentDevice.value?.let { device ->
            if (device.status != "ON") {
                repository.updateDeviceStatus(device.deviceId, "ON")
            }
        }
    }

    fun turnDeviceOff() {
        currentDevice.value?.let { device ->
            if (device.status != "OFF") {
                repository.updateDeviceStatus(device.deviceId, "OFF")
            }
        }
    }

    fun toggleDevice() {
        currentDevice.value?.let { device ->
            repository.toggleDevice(device)
        }
    }

    // ============= LIGHT-SPECIFIC =============

    fun setBrightness(brightness: Int) {
        (currentDevice.value as? Light)?.let { light ->
            repository.setLightBrightness(light, brightness)
        }
    }

    fun setLightSchedule(onTime: String, offTime: String) {
        (currentDevice.value as? Light)?.let { light ->
            repository.setLightSchedule(light, onTime, offTime)
        }
    }

    fun disableLightSchedule() {
        (currentDevice.value as? Light)?.let { light ->
            repository.disableLightSchedule(light)
        }
    }

    // ============= IRON-SPECIFIC =============

    fun setIronMaxDuration(maxMinutes: Int) {
        (currentDevice.value as? Iron)?.let { iron ->
            repository.setIronMaxDuration(iron, maxMinutes)
            repository.monitorIronDevice(iron)
        }
    }
}
