package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.smart_home.models.Device
import com.example.smart_home.models.Floor
import com.example.smart_home.models.MultiSwitch
import com.example.smart_home.repository.SmartHomeRepository

class FloorPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmartHomeRepository.getInstance(application)
    private val selectedFloorId = MutableLiveData<String>()
    val floors: LiveData<List<Floor>> = repository.getAllFloors()

    val currentFloor: LiveData<Floor> = selectedFloorId.switchMap { floorId ->
        repository.getFloorById(floorId)
    }

    val floorDevices: LiveData<List<Device>> = selectedFloorId.switchMap { floorId ->
        repository.getDevicesByFloor(floorId)
    }

    val unplacedDevices: LiveData<List<Device>> = repository.getAllDevices().switchMap { all ->
        val unplaced = all.filter { it.floorId.isEmpty() }
        MutableLiveData(unplaced)
    }

    fun selectFloor(floorId: String) {
        selectedFloorId.value = floorId
    }

    fun placeDevice(device: Device, x: Int, y: Int) {
        selectedFloorId.value?.let { floorId ->
            device.floorId = floorId
            device.gridX = x
            device.gridY = y
            repository.updateDevice(device)
        }
    }

    fun createNewDevice(name: String, room: String, type: String, x: Int, y: Int) {
        selectedFloorId.value?.let { floorId ->
            val deviceId = "dev_${System.currentTimeMillis()}"
            val device = Device(
                deviceId = deviceId,
                name = name,
                roomName = room,
                type = type,
                floorId = floorId,
                gridX = x,
                gridY = y,
                status = "OFF"
            )
            
            if (type == "MULTI_SWITCH") {
                device.switchCount = 3 // Default to 3 switches
                device.switches = mutableListOf(
                    MultiSwitch.SwitchState(1, "OFF"),
                    MultiSwitch.SwitchState(2, "OFF"),
                    MultiSwitch.SwitchState(3, "OFF")
                )
            }
            
            repository.addDevice(device)
        }
    }

    fun addFloor(floor: Floor) {
        repository.addFloor(floor)
    }

    fun addFloor(name: String) {
        val floorId = "floor_${System.currentTimeMillis()}"
        val floor = Floor(
            floorId = floorId,
            name = name,
            gridWidth = 4,
            gridHeight = 4
        )
        addFloor(floor)
    }

    fun updateFloor(floor: Floor) {
        repository.updateFloor(floor)
    }

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()
}
