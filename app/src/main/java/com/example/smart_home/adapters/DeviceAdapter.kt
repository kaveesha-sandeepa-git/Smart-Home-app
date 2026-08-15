package com.example.smart_home.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.models.Device

class DeviceAdapter(
    private var devices: List<Device>,
    private val listener: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    fun updateDevices(newDevices: List<Device>) {
        this.devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_devices, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device, listener)
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardRoot: View = view.findViewById(R.id.device_card_root)
        private val icon: ImageView = view.findViewById(R.id.device_icon)
        private val name: TextView = view.findViewById(R.id.device_name)
        private val type: TextView = view.findViewById(R.id.device_type)
        private val status: TextView = view.findViewById(R.id.device_status)
        private val toggleSwitch: androidx.appcompat.widget.SwitchCompat = view.findViewById(R.id.btn_toggle)

        fun bind(device: Device, listener: (Device) -> Unit) {
            name.text = device.name
            type.text = device.type

            // Set icon based on device type
            setDeviceIcon(device.type)

            // Set status pill text and background
            setStatusBadge(device.status)

            // Set toggle switch state
            // temporarily remove listener to prevent it from firing during bind
            toggleSwitch.setOnCheckedChangeListener(null)
            toggleSwitch.isChecked = device.status == "ON"
            toggleSwitch.isEnabled = device.status != "DISCONNECTED" && device.status != "ERROR"

            // Handle toggle switch
            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) "ON" else "OFF"
                device.status = newStatus
                setStatusBadge(newStatus)
                listener(device) // Just pass the device back, or maybe toggle isn't meant to open details
            }

            // Handle details button (now the whole card)
            cardRoot.setOnClickListener {
                listener(device)
            }
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
            
            // Set the background color based on the type, matching the HTML prototype
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
                else -> Pair(R.color.status_disconnected, R.drawable.status_chip_disc_bg) // DISCONNECTED
            }
            
            status.setTextColor(itemView.context.getColor(colorRes))
            status.setBackgroundResource(bgRes)
        }
    }
}
