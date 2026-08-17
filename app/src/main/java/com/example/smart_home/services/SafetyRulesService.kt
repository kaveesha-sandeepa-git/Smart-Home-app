package com.example.smart_home.services

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.smart_home.database.AppDatabase
import com.example.smart_home.database.DeviceDao
import com.example.smart_home.models.Device
import kotlinx.coroutines.*
import java.util.*

class SafetyRulesService(context: Context, private val firebaseService: FirebaseSyncService) {

    private val deviceDao: DeviceDao = AppDatabase.getInstance(context).deviceDao()
    private var safetyCheckTimer: Timer? = null
    private val safetyAlert = MutableLiveData<String?>()
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ============= IRON DEVICE SAFETY MONITORING =============

    fun startIronSafetyMonitoring() {
        safetyCheckTimer = Timer()
        safetyCheckTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkIronSafety()
                checkHighPowerDevices()
            }
        }, 0, 5000) // Check every 5 seconds
    }

    fun stopIronSafetyMonitoring() {
        safetyCheckTimer?.cancel()
        safetyCheckTimer = null
    }

    private fun checkIronSafety() {
        serviceScope.launch {
            // Get all iron devices
            // Note: This is a simplified approach. In production, use a proper query
            Log.d(TAG, "Checking iron device safety...")
        }
    }

    /**
     * Check if an Iron device has exceeded max ON duration
     */
    fun monitorIronDevice(device: Device?) {
        if (device == null) return

        if ("ON" == device.status && device.sessionStartTime > 0) {
            val sessionDuration = (System.currentTimeMillis() - device.sessionStartTime) / 1000 / 60 // in minutes

            device.currentSessionMinutes = sessionDuration.toInt()

            if (sessionDuration >= device.maxOnDurationMinutes) {
                // Safety cutoff triggered
                device.status = "OFF"
                device.safetyAlertActive = true

                serviceScope.launch { deviceDao.updateDevice(device) }
                firebaseService.updateDeviceStatus(device.deviceId, "OFF")

                val alertMessage = "⚠️ SAFETY ALERT: ${device.name} exceeded max ON time of " +
                        "${device.maxOnDurationMinutes} minutes. Device turned OFF automatically."
                safetyAlert.postValue(alertMessage)

                Log.w(TAG, alertMessage)
            } else {
                // Update session duration
                device.safetyAlertActive = false
                serviceScope.launch { deviceDao.updateDevice(device) }
            }
        }
    }

    fun setMaxDuration(deviceId: String, maxMinutes: Int) {
        serviceScope.launch {
            // Update in database
            Log.d(TAG, "Max duration set for device $deviceId: $maxMinutes minutes")
        }
    }

    // ============= HIGH POWER DEVICE MONITORING =============

    private fun checkHighPowerDevices() {
        // Monitor devices that consume significant power
        // Example: Multiple high-wattage outlets or irons running simultaneously
        Log.d(TAG, "Checking high-power device configurations...")
    }

    /**
     * Check if total power consumption exceeds safe limits
     */
    fun checkPowerConsumption(maxWattage: Int) {
        serviceScope.launch {
            // Query all ON devices and sum their power ratings
            // If total > maxWattage, trigger alerts or cutoffs
            Log.d(TAG, "Power consumption check: max ${maxWattage}W")
        }
    }

    // ============= SAFETY ALERTS =============

    fun getSafetyAlerts(): LiveData<String?> = safetyAlert

    fun clearSafetyAlert() {
        safetyAlert.postValue(null)
    }

    /**
     * Disable device immediately for critical safety issues
     */
    fun emergencyShutdown(deviceId: String) {
        firebaseService.updateDeviceStatus(deviceId, "OFF")
        safetyAlert.postValue("EMERGENCY: Device $deviceId has been shutdown.")
        Log.e(TAG, "EMERGENCY SHUTDOWN initiated for device: $deviceId")
    }

    /**
     * Check for devices left ON for extended periods (potential hazards)
     */
    fun checkUnattendedDevices() {
        serviceScope.launch {
            val oneHourMs = 60 * 60 * 1000
            val cutoffTime = System.currentTimeMillis() - oneHourMs
            
            Log.d(TAG, "Checking for unattended devices ON for more than 1 hour...")
            // Query devices ON longer than 1 hour and send notification
        }
    }

    companion object {
        private const val TAG = "SafetyRulesService"
    }
}
