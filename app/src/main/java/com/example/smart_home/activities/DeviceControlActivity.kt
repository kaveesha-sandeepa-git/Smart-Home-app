package com.example.smart_home.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.models.Device
import com.example.smart_home.models.SecurityCamera
import com.example.smart_home.viewmodels.DeviceControlViewModel

class DeviceControlActivity : AppCompatActivity() {

    private val viewModel: DeviceControlViewModel by viewModels()

    private lateinit var toolbar: Toolbar
    private lateinit var deviceIcon: ImageView
    private lateinit var deviceName: TextView
    private lateinit var deviceType: TextView
    private lateinit var currentStatus: TextView
    private lateinit var btnTurnOn: Button
    private lateinit var btnTurnOff: Button
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
    private lateinit var cardCamera: CardView
    private lateinit var playerView: PlayerView

    private lateinit var currentDevice: Device
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        deviceIcon = findViewById(R.id.device_icon)
        deviceName = findViewById(R.id.device_name)
        deviceType = findViewById(R.id.device_type)
        currentStatus = findViewById(R.id.device_status)
        btnTurnOn = findViewById(R.id.btn_turn_on)
        btnTurnOff = findViewById(R.id.btn_turn_off)
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
        cardCamera = findViewById(R.id.card_camera)
        playerView = findViewById(R.id.player_view)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Device Control"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load device from intent
        intent.getStringExtra("deviceId")?.let { deviceId ->
            viewModel.loadDevice(deviceId)
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.currentDevice.observe(this) { device ->
            device?.let {
                currentDevice = it
                updateUI()
            }
        }
    }

    private fun setupListeners() {
        btnTurnOn.setOnClickListener {
            viewModel.turnDeviceOn()
        }

        btnTurnOff.setOnClickListener {
            viewModel.turnDeviceOff()
        }

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
        deviceName.text = currentDevice.name
        deviceType.text = currentDevice.type
        currentStatus.text = "Status: ${currentDevice.status}"

        // Show/hide device-specific controls
        when (currentDevice.type) {
            "LIGHT" -> {
                cardBrightness.visibility = View.VISIBLE
                cardScheduling.visibility = View.VISIBLE
                cardMaxDuration.visibility = View.GONE
                cardMultiSwitch.visibility = View.GONE
                cardCamera.visibility = View.GONE
            }
            "IRON" -> {
                cardBrightness.visibility = View.GONE
                cardScheduling.visibility = View.GONE
                cardMaxDuration.visibility = View.VISIBLE
                cardMultiSwitch.visibility = View.GONE
                cardCamera.visibility = View.GONE
            }
            "MULTI_SWITCH" -> {
                cardBrightness.visibility = View.GONE
                cardScheduling.visibility = View.GONE
                cardMaxDuration.visibility = View.GONE
                cardMultiSwitch.visibility = View.VISIBLE
                cardCamera.visibility = View.GONE
            }
            "CAMERA" -> {
                cardBrightness.visibility = View.GONE
                cardScheduling.visibility = View.GONE
                cardMaxDuration.visibility = View.GONE
                cardMultiSwitch.visibility = View.GONE
                cardCamera.visibility = View.VISIBLE
                initializePlayer()
            }
            else -> {
                cardBrightness.visibility = View.GONE
                cardScheduling.visibility = View.GONE
                cardMaxDuration.visibility = View.GONE
                cardMultiSwitch.visibility = View.GONE
                cardCamera.visibility = View.GONE
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun initializePlayer() {
        val camera = currentDevice as? SecurityCamera ?: return
        if (camera.streamUrl.isEmpty()) return

        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()
            playerView.player = exoPlayer
        }

        val mediaItem = MediaItem.fromUri(camera.streamUrl)
        
        // Use HLS media source if the URL ends with .m3u8
        val mediaSource = if (camera.streamUrl.contains(".m3u8")) {
            HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                .createMediaSource(mediaItem)
        } else {
            null // Let ExoPlayer handle standard formats
        }

        if (mediaSource != null) {
            exoPlayer?.setMediaSource(mediaSource)
        } else {
            exoPlayer?.setMediaItem(mediaItem)
        }

        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
