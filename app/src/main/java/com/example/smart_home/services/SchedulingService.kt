package com.example.smart_home.services

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.smart_home.database.AppDatabase
import com.example.smart_home.database.DeviceDao
import com.example.smart_home.models.Light
import kotlinx.coroutines.*
import java.util.*

class SchedulingService(context: Context, private val firebaseService: FirebaseSyncService) {

    private val deviceDao: DeviceDao = AppDatabase.getInstance(context).deviceDao()
    private val alarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private var schedulingCheckTimer: Timer? = null
    private val scheduleEvent = MutableLiveData<String?>()
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ============= SCHEDULE MANAGEMENT =============

    fun startScheduleMonitoring() {
        schedulingCheckTimer = Timer()
        schedulingCheckTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkScheduledDevices()
            }
        }, 0, 60000) // Check every 1 minute
    }

    fun stopScheduleMonitoring() {
        schedulingCheckTimer?.cancel()
        schedulingCheckTimer = null
    }

    /**
     * Set schedule for a light device
     */
    fun setLightSchedule(light: Light, onTimeHHmm: String, offTimeHHmm: String) {
        serviceScope.launch {
            light.schedulingEnabled = true
            light.scheduleOnTime = parseTimeToMillis(onTimeHHmm)
            light.scheduleOffTime = parseTimeToMillis(offTimeHHmm)

            deviceDao.updateDevice(light)
            firebaseService.updateDevice(light)

            scheduleEvent.postValue("Schedule set for ${light.name}: ON at $onTimeHHmm, OFF at $offTimeHHmm")
            Log.d(TAG, "Schedule set for light: ${light.name}")
        }
    }

    /**
     * Disable scheduling for a device
     */
    fun disableSchedule(light: Light) {
        serviceScope.launch {
            light.schedulingEnabled = false
            deviceDao.updateDevice(light)
            firebaseService.updateDevice(light)

            scheduleEvent.postValue("Schedule disabled for ${light.name}")
            Log.d(TAG, "Schedule disabled for light: ${light.name}")
        }
    }

    /**
     * Check if any scheduled devices need to be turned ON/OFF
     */
    private fun checkScheduledDevices() {
        val now = Calendar.getInstance()
        
        serviceScope.launch {
            // In production, query all lights with scheduling enabled
            // For now, this is a placeholder
            Log.d(TAG, "Checking scheduled devices at ${now.time}")
        }
    }

    /**
     * Execute scheduled action for a device
     */
    fun executeScheduledAction(deviceId: String, action: String) {
        if ("ON" == action) {
            firebaseService.updateDeviceStatus(deviceId, "ON")
            scheduleEvent.postValue("Scheduled device $deviceId turned ON")
            Log.d(TAG, "Executed scheduled ON for device: $deviceId")
        } else if ("OFF" == action) {
            firebaseService.updateDeviceStatus(deviceId, "OFF")
            scheduleEvent.postValue("Scheduled device $deviceId turned OFF")
            Log.d(TAG, "Executed scheduled OFF for device: $deviceId")
        }
    }

    // ============= TIME UTILITIES =============

    /**
     * Convert HH:mm format to milliseconds since midnight
     */
    private fun parseTimeToMillis(timeHHmm: String): Long {
        return try {
            val parts = timeHHmm.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            ((hours * 60 + minutes) * 60 * 1000).toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse time: $timeHHmm", e)
            0
        }
    }

    /**
     * Convert milliseconds since midnight to HH:mm format
     */
    fun millisToTimeHHmm(millis: Long): String {
        val totalMinutes = (millis / 1000 / 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    /**
     * Get current time in milliseconds since midnight
     */
    private fun getCurrentTimeMs(): Long {
        val today = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return today.timeInMillis - midnight.timeInMillis
    }

    /**
     * Check if current time matches scheduled time (within 1 minute window)
     */
    private fun isTimeWindow(scheduledTimeMs: Long, windowMinutes: Long): Boolean {
        val currentTimeMs = getCurrentTimeMs()
        val windowMs = windowMinutes * 60 * 1000

        return Math.abs(currentTimeMs - scheduledTimeMs) < windowMs
    }

    // ============= SCHEDULE EVENTS =============

    fun getScheduleEvents(): LiveData<String?> = scheduleEvent

    fun clearScheduleEvent() {
        scheduleEvent.postValue(null)
    }

    /**
     * Get all devices with active schedules
     */
    fun getScheduledDevices(): LiveData<List<com.example.smart_home.models.Device>> {
        // Return list of devices with scheduling enabled
        return deviceDao.getAllDevices()
    }

    companion object {
        private const val TAG = "SchedulingService"
    }
}
