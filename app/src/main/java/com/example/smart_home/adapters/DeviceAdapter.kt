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
        private val icon: ImageView = view.findViewById(R.id.device_icon)
        private val name: TextView = view.findViewById(R.id.device_name)
        private val type: TextView = view.findViewById(R.id.device_type)
        private val status: TextView = view.findViewById(R.id.device_status)
        private val indicator: ImageView = view.findViewById(R.id.status_indicator)
        private val toggleBtn: Button = view.findViewById(R.id.btn_toggle)
        private val detailsBtn: Button = view.findViewById(R.id.btn_details)

        fun bind(device: Device, listener: (Device) -> Unit) {
            name.text = device.name
            type.text = device.type
            setDeviceStatus(device.status)

            // Set icon based on device type
            setDeviceIcon(device.type)

            // Set status indicator color
            setStatusIndicator(device.status)

            // Handle toggle button
            toggleBtn.setOnClickListener {
                device.toggleStatus()
                setDeviceStatus(device.status)
                setStatusIndicator(device.status)
                listener(device)
            }

            // Handle details button
            detailsBtn.setOnClickListener {
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
        }

        private fun setStatusIndicator(status: String) {
            val colorRes = when (status) {
                "ON" -> R.color.status_on
                "OFF" -> R.color.status_off
                "ERROR" -> R.color.status_error
                else -> R.color.status_disconnected
            }
            val color = itemView.context.getColor(colorRes)
            indicator.setImageResource(android.R.drawable.presence_online)
            indicator.imageTintList = ColorStateList.valueOf(color)
        }

        private fun setDeviceStatus(statusText: String) {
            status.text = "Status: $statusText"
        }
    }
}
