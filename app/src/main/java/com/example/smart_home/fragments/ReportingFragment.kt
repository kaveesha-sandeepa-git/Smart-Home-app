package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smart_home.repository.SmartHomeRepository
import java.util.*
import kotlin.concurrent.schedule
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
    private lateinit var repository: SmartHomeRepository
    private var currentObserver: androidx.lifecycle.LiveData<List<DeviceUsageReport>>? = null

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

        repository = SmartHomeRepository.getInstance(requireContext())

        // Load all reports initially
        currentObserver = repository.getAllReports()
        currentObserver?.observe(viewLifecycleOwner) { list ->
            reports.clear()
            reports.addAll(list)
            reportAdapter.notifyDataSetChanged()
        }

        btnToday.setOnClickListener { filterReports("today") }
        btnWeek.setOnClickListener { filterReports("week") }
        btnMonth.setOnClickListener { filterReports("month") }
    }

    private fun filterReports(period: String) {
        // compute start and end times
        val now = Calendar.getInstance()
        val end = now.timeInMillis
        val start = when (period) {
            "today" -> {
                now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0)
                now.timeInMillis
            }
            "week" -> {
                now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek); now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0)
                now.timeInMillis
            }
            "month" -> {
                now.set(Calendar.DAY_OF_MONTH, 1); now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0)
                now.timeInMillis
            }
            else -> 0L
        }

        currentObserver?.removeObservers(viewLifecycleOwner)
        currentObserver = repository.getReportsByDateRange(start, end)
        currentObserver?.observe(viewLifecycleOwner) { list ->
            reports.clear()
            reports.addAll(list)
            reportAdapter.notifyDataSetChanged()
        }
    }

    companion object {
        fun newInstance() = ReportingFragment()
    }
}
