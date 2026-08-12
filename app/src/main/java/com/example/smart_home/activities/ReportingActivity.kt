package com.example.smart_home.activities

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.adapters.UsageReportAdapter
import com.example.smart_home.models.DeviceUsageReport

class ReportingActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var reportsList: RecyclerView
    private lateinit var reportAdapter: UsageReportAdapter
    private var reports: MutableList<DeviceUsageReport> = mutableListOf()
    private lateinit var btnToday: Button
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporting)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        reportsList = findViewById(R.id.reports_list)
        btnToday = findViewById(R.id.btn_today)
        btnWeek = findViewById(R.id.btn_week)
        btnMonth = findViewById(R.id.btn_month)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Usage Reports"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup RecyclerView
        reportAdapter = UsageReportAdapter(reports)
        reportsList.layoutManager = LinearLayoutManager(this)
        reportsList.adapter = reportAdapter

        // Load sample reports
        loadSampleReports()

        // Setup button listeners
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
