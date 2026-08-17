package com.example.smart_home.activities

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import androidx.activity.viewModels
import com.example.smart_home.adapters.SwitchUnitAdapter
import com.example.smart_home.models.Device
import com.example.smart_home.viewmodels.DeviceControlViewModel

class DeviceControlActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var deviceIcon: ImageView
    private lateinit var deviceName: TextView
    private lateinit var deviceType: TextView
    private lateinit var currentStatus: TextView
    private lateinit var powerToggle: SwitchCompat
    private lateinit var btnSetDuration: Button
    private lateinit var btnSaveSchedule: Button
    private lateinit var schedulingToggle: androidx.appcompat.widget.SwitchCompat
    private lateinit var onTimeInput: EditText
    private lateinit var offTimeInput: EditText
    private lateinit var brightnessSlider: SeekBar
    private lateinit var brightnessValue: TextView
    private lateinit var maxDurationInput: EditText
    private lateinit var switchesList: RecyclerView
    private lateinit var btnRemoveDevice: Button

    private lateinit var cardBrightness: CardView
    private lateinit var cardScheduling: CardView
    private lateinit var cardMaxDuration: CardView
    private lateinit var cardMultiSwitch: CardView
    private lateinit var cardUsageStats: CardView
    private lateinit var sessionOnVal: TextView
    private lateinit var totalOnVal: TextView

    private val viewModel: DeviceControlViewModel by viewModels()
    private var currentDevice: Device? = null
    private lateinit var switchAdapter: SwitchUnitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        deviceIcon = findViewById(R.id.device_icon)
        deviceName = findViewById(R.id.device_name)
        deviceType = findViewById(R.id.device_type)
        currentStatus = findViewById(R.id.device_status)
        powerToggle = findViewById(R.id.power_toggle)
        brightnessSlider = findViewById(R.id.brightness_slider)
        brightnessValue = findViewById(R.id.brightness_value)
        schedulingToggle = findViewById(R.id.scheduling_toggle)
        onTimeInput = findViewById(R.id.input_on_time)
        offTimeInput = findViewById(R.id.input_off_time)
        btnSaveSchedule = findViewById(R.id.btn_save_schedule)
        maxDurationInput = findViewById(R.id.max_duration_input)
        btnSetDuration = findViewById(R.id.btn_set_duration)
        switchesList = findViewById(R.id.switches_list)
        btnRemoveDevice = findViewById(R.id.btn_remove_device)

        cardBrightness = findViewById(R.id.card_brightness)
        cardScheduling = findViewById(R.id.card_scheduling)
        cardMaxDuration = findViewById(R.id.card_max_duration)
        cardMultiSwitch = findViewById(R.id.card_multi_switch)
        cardUsageStats = findViewById(R.id.card_usage_stats)
        sessionOnVal = findViewById(R.id.session_on_val)
        totalOnVal = findViewById(R.id.total_on_val)

        switchesList.layoutManager = LinearLayoutManager(this)
        switchAdapter = SwitchUnitAdapter(emptyList()) { index ->
            viewModel.toggleMultiSwitch(index)
        }
        switchesList.adapter = switchAdapter

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Device Control"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load device from intent (if provided) or fall back to sample
        val deviceId = intent.getStringExtra("deviceId")
        if (!deviceId.isNullOrEmpty()) {
            viewModel.loadDevice(deviceId)
            viewModel.currentDevice.observe(this) { device ->
                device?.let {
                    currentDevice = it
                    updateUI()
                }
            }
        } else {
            loadSampleDevice()
        }

        setupListeners()
    }

    private fun loadSampleDevice() {
        currentDevice = com.example.smart_home.models.Device(
            deviceId = "light1",
            name = "Living Room Light",
            roomName = "Living Room",
            type = "LIGHT",
            status = "ON",
            floorId = "floor1",
            gridX = 1,
            gridY = 0
        )
        updateUI()
    }

    private fun setupListeners() {
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                brightnessValue.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        btnSetDuration.setOnClickListener {
            val durationStr = maxDurationInput.text.toString()
            if (durationStr.isNotEmpty()) {
                val duration = durationStr.toInt()
                viewModel.setIronMaxDuration(duration)
                Toast.makeText(this, "Max duration set to $duration mins", Toast.LENGTH_SHORT).show()
            }
        }

        onTimeInput.setOnClickListener {
            showTimePicker(onTimeInput)
        }

        offTimeInput.setOnClickListener {
            showTimePicker(offTimeInput)
        }

        btnSaveSchedule.setOnClickListener {
            val onTime = onTimeInput.text.toString()
            val offTime = offTimeInput.text.toString()
            if (onTime.contains(":") && offTime.contains(":")) {
                viewModel.setLightSchedule(onTime, offTime)
                Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid time format (HH:mm)", Toast.LENGTH_SHORT).show()
            }
        }

        schedulingToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // If turning ON, ensure we save the current inputs
                val onTime = onTimeInput.text.toString()
                val offTime = offTimeInput.text.toString()
                viewModel.setLightSchedule(onTime, offTime)
            } else {
                viewModel.disableLightSchedule()
            }
        }

        btnRemoveDevice.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        val deviceName = currentDevice?.name ?: "this device"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Device")
            .setMessage("Are you sure you want to remove $deviceName? This action cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.deleteDevice()
                Toast.makeText(this, "Device removed", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setStatusBadge(statusText: String) {
        currentStatus.text = statusText
        val (textColor, bgRes) = when (statusText) {
            "ON" -> Pair(R.color.status_on, R.drawable.status_chip_on_bg)
            "OFF" -> Pair(R.color.status_off, R.drawable.status_chip_off_bg)
            "ERROR" -> Pair(R.color.status_error, R.drawable.status_chip_error_bg)
            else -> Pair(R.color.status_disconnected, R.drawable.status_chip_disc_bg)
        }
        currentStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, textColor))
        currentStatus.setBackgroundResource(bgRes)
    }

    private fun updateUI() {
        val device = currentDevice
        deviceName.text = device?.name ?: ""
        
        // Update room name if available
        if (device?.roomName?.isNotEmpty() == true) {
            deviceName.text = "${device.name} (${device.roomName})"
        } else {
            deviceName.text = device?.name ?: ""
        }
        
        deviceType.text = device?.type ?: ""

        val statusText = device?.status ?: "UNKNOWN"
        setStatusBadge(statusText)

        // Update Usage Stats
        device?.let { d ->
            val totalMs = d.totalOnTime + if (d.status == "ON" && d.sessionStartTime > 0) {
                System.currentTimeMillis() - d.sessionStartTime
            } else 0
            
            val sessionMs = if (d.status == "ON" && d.sessionStartTime > 0) {
                System.currentTimeMillis() - d.sessionStartTime
            } else 0
            
            totalOnVal.text = viewModel.formatDuration(totalMs)
            sessionOnVal.text = viewModel.formatDuration(sessionMs)
        }

        powerToggle.setOnCheckedChangeListener(null)
        powerToggle.isChecked = statusText == "ON"
        powerToggle.isEnabled = statusText != "DISCONNECTED" && statusText != "ERROR"
        powerToggle.setOnCheckedChangeListener { _, isChecked ->
            currentDevice?.let { d ->
                if (d.status == "DISCONNECTED" || d.status == "ERROR") return@let
                
                // Immediate local update for better UX
                val oldStatus = d.status
                val newStatus = if (isChecked) "ON" else "OFF"
                if (oldStatus != newStatus) {
                    d.status = newStatus
                    setStatusBadge(newStatus)
                    if (isChecked) viewModel.turnDeviceOn() else viewModel.turnDeviceOff()
                }
            }
        }

        // Show/hide device-specific controls
        device?.let { d ->
            schedulingToggle.setOnCheckedChangeListener(null)
            schedulingToggle.isChecked = d.schedulingEnabled
            // Re-register listener is handled in setupListeners once, but since we are in updateUI
            // which can be called multiple times, we need to be careful with listeners.
            // Better to only set values here.
            onTimeInput.setText(viewModel.formatMillisToTime(d.scheduleOnTime))
            offTimeInput.setText(viewModel.formatMillisToTime(d.scheduleOffTime))
            
            // Re-setup listener
            schedulingToggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.setLightSchedule(onTimeInput.text.toString(), offTimeInput.text.toString())
                } else {
                    viewModel.disableLightSchedule()
                }
            }
        }

        when (device?.type) {
            "LIGHT" -> {
                cardBrightness.visibility = View.VISIBLE
                cardScheduling.visibility = View.VISIBLE
                cardMaxDuration.visibility = View.GONE
                cardMultiSwitch.visibility = View.GONE
            }
            "IRON" -> {
                cardBrightness.visibility = View.GONE
                cardScheduling.visibility = View.GONE
                cardMaxDuration.visibility = View.VISIBLE
                cardMultiSwitch.visibility = View.GONE
            }
            "MULTI_SWITCH" -> {
                cardBrightness.visibility = View.GONE
                cardScheduling.visibility = View.GONE
                cardMaxDuration.visibility = View.GONE
                cardMultiSwitch.visibility = View.VISIBLE
                
                device?.let {
                    switchAdapter.updateSwitches(it.switches)
                }
            }
            else -> {
                cardBrightness.visibility = View.GONE
                cardScheduling.visibility = View.GONE
                cardMaxDuration.visibility = View.GONE
                cardMultiSwitch.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showTimePicker(editText: EditText) {
        val currentTime = editText.text.toString()
        var hour = 18
        var minute = 0
        
        if (currentTime.contains(":")) {
            val parts = currentTime.split(":")
            hour = parts[0].toIntOrNull() ?: 18
            minute = parts[1].toIntOrNull() ?: 0
        }

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            editText.setText(formattedTime)
        }, hour, minute, true).show()
    }
}
