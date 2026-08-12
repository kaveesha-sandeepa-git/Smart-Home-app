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
        private val deviceName: TextView = view.findViewById(R.id.report_device_name)
        private val totalOnTime: TextView = view.findViewById(R.id.total_on_time_report)
        private val toggles: TextView = view.findViewById(R.id.toggles_report)
        private val energyUsed: TextView = view.findViewById(R.id.energy_used_report)

        fun bind(report: DeviceUsageReport) {
            deviceName.text = report.deviceName
            totalOnTime.text = report.getTotalOnTimeFormatted()
            toggles.text = report.dailyToggleCount.toString()
            energyUsed.text = String.format("%.2f kWh", report.energyConsumedKwh)
        }
    }
}
