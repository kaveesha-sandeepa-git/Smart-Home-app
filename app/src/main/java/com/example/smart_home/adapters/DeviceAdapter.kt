package com.example.smart_home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.models.Device

class DeviceAdapter(
    private var devices: List<Device>,
    private val toggleListener: (Device) -> Unit,
    private val detailsListener: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var floorMap: Map<String, String> = emptyMap()
    private var showFloorNote: Boolean = false

    fun updateDevices(newDevices: List<Device>, floorMap: Map<String, String>, showFloorNote: Boolean) {
        this.devices = newDevices
        this.floorMap = floorMap
        this.showFloorNote = showFloorNote
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_devices, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val floorName = floorMap[device.floorId] ?: "Unknown Floor"
        holder.bind(device, floorName, showFloorNote, toggleListener, detailsListener)
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardRoot: View = view.findViewById(R.id.device_card_root)
        private val icon: ImageView = view.findViewById(R.id.device_icon)
        private val name: TextView = view.findViewById(R.id.device_name)
        private val type: TextView = view.findViewById(R.id.device_type)
        private val roomName: TextView = view.findViewById(R.id.device_room)
        private val floorNote: TextView = view.findViewById(R.id.device_floor_note)
        private val status: TextView = view.findViewById(R.id.device_status)
        private val schedule: TextView = view.findViewById(R.id.device_schedule)
        private val toggleSwitch: androidx.appcompat.widget.SwitchCompat = view.findViewById(R.id.btn_toggle)

        fun bind(
            device: Device, 
            floorNameStr: String, 
            showFloor: Boolean,
            toggleListener: (Device) -> Unit, 
            detailsListener: (Device) -> Unit
        ) {
            name.text = device.name
            type.text = device.type
            roomName.text = if (device.roomName.isNotBlank()) "• ${device.roomName}" else ""

            if (showFloor) {
                floorNote.text = "• $floorNameStr"
                floorNote.visibility = View.VISIBLE
            } else {
                floorNote.visibility = View.GONE
            }

            setDeviceIcon(device.type)
            setStatusBadge(device.status)
            
            if (device.schedulingEnabled) {
                val onTime = formatMillisToTime(device.scheduleOnTime)
                val offTime = formatMillisToTime(device.scheduleOffTime)
                schedule.text = "🕒 Schedule: ON ($onTime - $offTime)"
                schedule.visibility = View.VISIBLE
            } else {
                schedule.visibility = View.GONE
            }

            toggleSwitch.setOnCheckedChangeListener(null)
            toggleSwitch.isChecked = device.status == "ON"
            toggleSwitch.isEnabled = device.status != "DISCONNECTED" && device.status != "ERROR"

            toggleSwitch.setOnCheckedChangeListener { _, _ ->
                toggleListener(device)
            }

            icon.setOnClickListener { detailsListener(device) }
            cardRoot.setOnClickListener { detailsListener(device) }
        }

        private fun setDeviceIcon(type: String) {
            val iconRes = when (type) {
                "OUTLET" -> R.drawable.ic_outlet
                "LIGHT" -> R.drawable.ic_light
                "CAMERA" -> R.drawable.ic_camera
                "IRON" -> R.drawable.ic_iron
                "MULTI_SWITCH" -> R.drawable.ic_switch
                else -> R.drawable.ic_device
            }
            icon.setImageResource(iconRes)
            
            val bgRes = when (type) {
                "LIGHT" -> R.drawable.icon_badge_light_bg
                "OUTLET" -> R.drawable.icon_badge_outlet_bg
                "IRON" -> R.drawable.icon_badge_iron_bg
                "CAMERA" -> R.drawable.icon_badge_camera_bg
                "MULTI_SWITCH" -> R.drawable.icon_badge_switch_bg
                else -> R.drawable.icon_badge_light_bg
            }
            icon.setBackgroundResource(bgRes)
        }

        private fun setStatusBadge(statusText: String) {
            status.text = statusText
            val (colorRes, bgRes) = when (statusText) {
                "ON" -> Pair(R.color.status_on, R.drawable.status_chip_on_bg)
                "OFF" -> Pair(R.color.status_off, R.drawable.status_chip_off_bg)
                "ERROR" -> Pair(R.color.status_error, R.drawable.status_chip_error_bg)
                else -> Pair(R.color.status_disconnected, R.drawable.status_chip_disc_bg)
            }
            status.setTextColor(itemView.context.getColor(colorRes))
            status.setBackgroundResource(bgRes)
        }

        private fun formatMillisToTime(millis: Long): String {
            val totalMinutes = (millis / 1000 / 60).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return String.format(java.util.Locale.US, "%02d:%02d", hours, minutes)
        }
    }
}
