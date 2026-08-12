package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.smart_home.models.DeviceUsageReport
import com.example.smart_home.repository.SmartHomeRepository
import java.util.*

/**
 * ViewModel for Reporting screen
 * Handles usage reports and statistics
 */
class ReportingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance(application)
    val allReports: LiveData<List<DeviceUsageReport>> = repository.getAllReports()
    
    private val filterTrigger = MutableLiveData<FilterParams>()
    
    val filteredReports: LiveData<List<DeviceUsageReport>> = filterTrigger.switchMap { params ->
        if (params.period == "all") {
            allReports
        } else {
            repository.getReportsByDateRange(params.startTime, params.endTime)
        }
    }

    init {
        filterByToday()
    }

    data class FilterParams(val period: String, val startTime: Long = 0, val endTime: Long = Long.MAX_VALUE)

    // ============= GETTERS =============

    fun getReportsByDevice(deviceId: String): LiveData<List<DeviceUsageReport>> =
        repository.getReportsByDevice(deviceId)

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()

    // ============= FILTERING =============

    fun filterByToday() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        filterTrigger.value = FilterParams("today", startOfDay, endOfDay)
    }

    fun filterByWeek() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val startOfWeek = cal.timeInMillis
        val endOfWeek = System.currentTimeMillis()

        filterTrigger.value = FilterParams("week", startOfWeek, endOfWeek)
    }

    fun filterByMonth() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        val startOfMonth = cal.timeInMillis
        val endOfMonth = System.currentTimeMillis()

        filterTrigger.value = FilterParams("month", startOfMonth, endOfMonth)
    }

    fun filterByAll() {
        filterTrigger.value = FilterParams("all")
    }

    // ============= REPORTING =============

    fun generateReport(deviceId: String) {
        // This is a bit inefficient as it observes forever or needs a owner. 
        // In a real app, we'd have a getDeviceByIdSync or similar.
        // For now, I'll follow the pattern of triggering it via repository if possible.
        repository.getAllDevices().observeForever(object : androidx.lifecycle.Observer<List<com.example.smart_home.models.Device>> {
            override fun onChanged(devices: List<com.example.smart_home.models.Device>) {
                devices.find { it.deviceId == deviceId }?.let {
                    repository.generateDeviceReport(it)
                }
                repository.getAllDevices().removeObserver(this)
            }
        })
    }
}
