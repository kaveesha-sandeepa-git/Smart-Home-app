package com.example.smart_home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.models.DeviceUsageReport

class UsageReportAdapter(
    private val reports: List<DeviceUsageReport>
) : RecyclerView.Adapter<UsageReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.bind(report)
    }

    override fun getItemCount(): Int = reports.size

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = view.findViewById(R.id.device_name)
        private val roomName: TextView = view.findViewById(R.id.room_name)
        private val floorName: TextView = view.findViewById(R.id.floor_name)
        private val totalOnTime: TextView = view.findViewById(R.id.stat_total_on)
        private val toggles: TextView = view.findViewById(R.id.stat_toggles)
        private val energyUsed: TextView = view.findViewById(R.id.stat_energy)
        private val status: TextView = view.findViewById(R.id.device_status)

        fun bind(report: DeviceUsageReport) {
            deviceName.text = report.deviceName
            roomName.text = if (report.roomName.isNotBlank()) "• ${report.roomName}" else ""
            floorName.text = "• ${report.floorName}"
            totalOnTime.text = report.getTotalOnTimeFormatted()
            toggles.text = report.dailyToggleCount.toString()
            energyUsed.text = String.format("%.2f kWh", report.energyConsumedKwh)
            
            // Set status chip
            status.text = report.status
            val (colorRes, bgRes) = when (report.status) {
                "ON" -> Pair(R.color.status_on, R.drawable.status_chip_on_bg)
                "OFF" -> Pair(R.color.status_off, R.drawable.status_chip_off_bg)
                else -> Pair(R.color.status_disconnected, R.drawable.status_chip_disc_bg)
            }
            status.setTextColor(itemView.context.getColor(colorRes))
            status.setBackgroundResource(bgRes)
        }
    }
}
