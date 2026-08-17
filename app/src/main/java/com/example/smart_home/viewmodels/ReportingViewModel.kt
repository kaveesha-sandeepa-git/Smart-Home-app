package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.smart_home.models.DeviceUsageReport
import com.example.smart_home.models.Floor
import com.example.smart_home.repository.SmartHomeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for Reporting screen
 * Handles usage reports and statistics
 */
class ReportingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance(application)
    
    private val _usageData = MutableLiveData<List<DeviceUsageReport>>()
    val usageData: LiveData<List<DeviceUsageReport>> = _usageData
    
    val floors: LiveData<List<Floor>> = repository.getAllFloors()
    
    private var currentPeriod = "today"
    private var selectedFloorId: String? = null // null means "All Floors"
    
    private var refreshJob: Job? = null

    init {
        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                calculateUsageForPeriod(currentPeriod, selectedFloorId)
                delay(5.seconds) // Refresh every 5 seconds
            }
        }
    }

    fun filterByToday() {
        currentPeriod = "today"
        calculateUsageForPeriod(currentPeriod, selectedFloorId)
    }

    fun filterByWeek() {
        currentPeriod = "week"
        calculateUsageForPeriod(currentPeriod, selectedFloorId)
    }

    fun filterByMonth() {
        currentPeriod = "month"
        calculateUsageForPeriod(currentPeriod, selectedFloorId)
    }

    fun selectFloor(floorId: String?) {
        selectedFloorId = if (floorId == "all") null else floorId
        calculateUsageForPeriod(currentPeriod, selectedFloorId)
    }

    private fun calculateUsageForPeriod(period: String, floorId: String?) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val end = now.timeInMillis
            val start = when (period) {
                "today" -> {
                    val cal = now.clone() as Calendar
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                "week" -> {
                    val cal = now.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    cal.timeInMillis
                }
                "month" -> {
                    val cal = now.clone() as Calendar
                    cal.add(Calendar.MONTH, -1)
                    cal.timeInMillis
                }
                else -> 0L
            }

            val usageMap = repository.getAllDeviceUsage(start, end)
            var allDevices = repository.getAllDevicesSync()
            
            // Filter by floor if needed
            if (floorId != null) {
                allDevices = allDevices.filter { it.floorId == floorId }
            }
            
            val reports = allDevices.map { device ->
                val duration = usageMap[device.deviceId] ?: 0L
                val floor = floors.value?.find { it.floorId == device.floorId }
                DeviceUsageReport(
                    reportId = "${period}_${device.deviceId}",
                    deviceId = device.deviceId,
                    deviceName = device.name,
                    roomName = device.roomName,
                    floorName = floor?.name ?: "Unknown",
                    totalOnTimeMs = duration,
                    energyConsumedKwh = repository.calculateEnergy(device, duration),
                    status = device.status,
                    generatedAt = System.currentTimeMillis()
                )
            }.sortedByDescending { it.totalOnTimeMs }
            
            _usageData.postValue(reports)
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()
}
