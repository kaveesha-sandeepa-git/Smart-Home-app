package com.example.smart_home.services

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.smart_home.database.AppDatabase
import com.example.smart_home.database.DeviceDao
import com.example.smart_home.models.Device
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
    fun setLightSchedule(device: Device, onTimeHHmm: String, offTimeHHmm: String) {
        serviceScope.launch {
            device.schedulingEnabled = true
            device.scheduleOnTime = parseTimeToMillis(onTimeHHmm)
            device.scheduleOffTime = parseTimeToMillis(offTimeHHmm)

            deviceDao.updateDevice(device)
            firebaseService.updateDevice(device)

            scheduleEvent.postValue("Schedule set for ${device.name}: ON at $onTimeHHmm, OFF at $offTimeHHmm")
            Log.d(TAG, "Schedule set for light: ${device.name}")
        }
    }

    /**
     * Disable scheduling for a device
     */
    fun disableSchedule(device: Device) {
        serviceScope.launch {
            device.schedulingEnabled = false
            deviceDao.updateDevice(device)
            firebaseService.updateDevice(device)

            scheduleEvent.postValue("Schedule disabled for ${device.name}")
            Log.d(TAG, "Schedule disabled for light: ${device.name}")
        }
    }

    /**
     * Check if any scheduled devices need to be turned ON/OFF
     */
    private fun checkScheduledDevices() {
        val currentTimeMs = getCurrentTimeMs()
        
        serviceScope.launch {
            val scheduledDevices = deviceDao.getScheduledDevicesSync()
            for (device in scheduledDevices) {
                val onTime = device.scheduleOnTime
                val offTime = device.scheduleOffTime
                
                val shouldBeOn = if (onTime < offTime) {
                    // Normal range (e.g., 08:00 to 17:00)
                    currentTimeMs in onTime until offTime
                } else {
                    // Range wraps midnight (e.g., 22:00 to 06:00)
                    currentTimeMs >= onTime || currentTimeMs < offTime
                }
                
                if (shouldBeOn && device.status != "ON") {
                    executeScheduledAction(device.deviceId, "ON")
                } else if (!shouldBeOn && device.status == "ON") {
                    // Only turn OFF if it's currently ON and was supposed to be ON by schedule?
                    // Actually, if schedule is enabled, it should strictly follow the state.
                    executeScheduledAction(device.deviceId, "OFF")
                }
            }
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
