package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.adapters.UsageReportAdapter
import com.example.smart_home.models.DeviceUsageReport
import com.example.smart_home.models.Floor
import com.example.smart_home.viewmodels.ReportingViewModel

/**
 * Reporting Fragment - Shows device usage reports with floor/period filters
 */
class ReportingFragment : Fragment() {

    private val viewModel: ReportingViewModel by viewModels()
    private lateinit var reportsList: RecyclerView
    private lateinit var reportAdapter: UsageReportAdapter
    private lateinit var floorSpinner: Spinner
    private lateinit var btnToday: Button
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button
    private val reports: MutableList<DeviceUsageReport> = mutableListOf()
    private var floors: List<Floor> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reporting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportsList = view.findViewById(R.id.reports_list)
        floorSpinner = view.findViewById(R.id.floor_spinner)
        btnToday = view.findViewById(R.id.btn_today)
        btnWeek = view.findViewById(R.id.btn_week)
        btnMonth = view.findViewById(R.id.btn_month)

        reportAdapter = UsageReportAdapter(reports)
        reportsList.layoutManager = LinearLayoutManager(requireContext())
        reportsList.adapter = reportAdapter

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.usageData.observe(viewLifecycleOwner) { list ->
            reports.clear()
            reports.addAll(list)
            reportAdapter.notifyDataSetChanged()
        }

        viewModel.floors.observe(viewLifecycleOwner) { floorList ->
            floors = floorList
            val names = mutableListOf("All Floors")
            names.addAll(floorList.map { it.name })
            
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            floorSpinner.adapter = adapter
        }
    }

    private fun setupListeners() {
        btnToday.setOnClickListener { viewModel.filterByToday() }
        btnWeek.setOnClickListener { viewModel.filterByWeek() }
        btnMonth.setOnClickListener { viewModel.filterByMonth() }

        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    viewModel.selectFloor("all")
                } else if (position - 1 in floors.indices) {
                    viewModel.selectFloor(floors[position - 1].floorId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    companion object {
        fun newInstance() = ReportingFragment()
    }
}
