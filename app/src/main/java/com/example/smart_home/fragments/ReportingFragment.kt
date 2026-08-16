package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.adapters.UsageReportAdapter
import com.example.smart_home.models.DeviceUsageReport

/**
 * Reporting Fragment - Shows device usage reports with floor/period filters
 */
class ReportingFragment : Fragment() {

    private lateinit var reportsList: RecyclerView
    private lateinit var reportAdapter: UsageReportAdapter
    private lateinit var btnToday: Button
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button
    private val reports: MutableList<DeviceUsageReport> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reporting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportsList = view.findViewById(R.id.reports_list)
        btnToday = view.findViewById(R.id.btn_today)
        btnWeek = view.findViewById(R.id.btn_week)
        btnMonth = view.findViewById(R.id.btn_month)

        reportAdapter = UsageReportAdapter(reports)
        reportsList.layoutManager = LinearLayoutManager(requireContext())
        reportsList.adapter = reportAdapter

        loadSampleReports()

        btnToday.setOnClickListener { filterReports("today") }
        btnWeek.setOnClickListener { filterReports("week") }
        btnMonth.setOnClickListener { filterReports("month") }
    }

    private fun loadSampleReports() {
        reports.clear()
        reports.add(DeviceUsageReport("report1", "light1", "Living Room Light"))
        reports.add(DeviceUsageReport("report2", "outlet1", "Kitchen Outlet"))
        reports.add(DeviceUsageReport("report3", "camera1", "Front Door Camera"))
        reportAdapter.notifyDataSetChanged()
    }

    private fun filterReports(period: String) {
        // TODO: Implement filtering logic
    }

    companion object {
        fun newInstance() = ReportingFragment()
    }
}
