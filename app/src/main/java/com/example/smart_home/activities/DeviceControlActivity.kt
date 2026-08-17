package com.example.smart_home.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import androidx.activity.viewModels
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
    private lateinit var schedulingToggle: androidx.appcompat.widget.SwitchCompat
    private lateinit var brightnessSlider: SeekBar
    private lateinit var brightnessValue: TextView
    private lateinit var maxDurationInput: EditText
    private lateinit var switchesList: RecyclerView

    private lateinit var cardBrightness: CardView
    private lateinit var cardScheduling: CardView
    private lateinit var cardMaxDuration: CardView
    private lateinit var cardMultiSwitch: CardView
    private lateinit var cardUsageStats: CardView

    private val viewModel: DeviceControlViewModel by viewModels()
    private var currentDevice: Device? = null

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
        maxDurationInput = findViewById(R.id.max_duration_input)
        btnSetDuration = findViewById(R.id.btn_set_duration)
        switchesList = findViewById(R.id.switches_list)

        cardBrightness = findViewById(R.id.card_brightness)
        cardScheduling = findViewById(R.id.card_scheduling)
        cardMaxDuration = findViewById(R.id.card_max_duration)
        cardMultiSwitch = findViewById(R.id.card_multi_switch)
        cardUsageStats = findViewById(R.id.card_usage_stats)

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
        currentDevice = com.example.smart_home.models.Light("light1", "Living Room Light", "ON", "floor1", 1, 0)
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
                // TODO: Set max duration for Iron devices
            }
        }
    }

    private fun updateUI() {
        val device = currentDevice
        deviceName.text = device?.name ?: ""
        deviceType.text = device?.type ?: ""

        val statusText = device?.status ?: "UNKNOWN"
        currentStatus.text = statusText

        val (textColor, bgRes) = when (statusText) {
            "ON" -> Pair(R.color.status_on, R.drawable.status_chip_on_bg)
            "OFF" -> Pair(R.color.status_off, R.drawable.status_chip_off_bg)
            "ERROR" -> Pair(R.color.status_error, R.drawable.status_chip_error_bg)
            else -> Pair(R.color.status_disconnected, R.drawable.status_chip_disc_bg)
        }
        currentStatus.setTextColor(ContextCompat.getColor(this, textColor))
        currentStatus.setBackgroundResource(bgRes)

        powerToggle.setOnCheckedChangeListener(null)
        powerToggle.isChecked = statusText == "ON"
        powerToggle.isEnabled = statusText != "DISCONNECTED" && statusText != "ERROR"
        powerToggle.setOnCheckedChangeListener { _, isChecked ->
            currentDevice?.let { d ->
                if (d.status == "DISCONNECTED" || d.status == "ERROR") return@let
                d.status = if (isChecked) "ON" else "OFF"
                if (isChecked) viewModel.turnDeviceOn() else viewModel.turnDeviceOff()
                updateUI()
            }
        }

        // Show/hide device-specific controls
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
}
