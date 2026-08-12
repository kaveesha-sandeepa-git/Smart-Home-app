package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.smart_home.models.Device
import com.example.smart_home.models.Floor
import com.example.smart_home.repository.SmartHomeRepository

class FloorPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmartHomeRepository.getInstance(application)
    private val selectedFloorId = MutableLiveData<String>()

    val currentFloor: LiveData<Floor> = selectedFloorId.switchMap { floorId ->
        repository.getFloorById(floorId)
    }

    val floorDevices: LiveData<List<Device>> = selectedFloorId.switchMap { floorId ->
        repository.getDevicesByFloor(floorId)
    }

    fun selectFloor(floorId: String) {
        selectedFloorId.value = floorId
    }

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()
}
