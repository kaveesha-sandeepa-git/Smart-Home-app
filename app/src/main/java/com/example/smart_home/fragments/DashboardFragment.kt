package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_home.R
import com.example.smart_home.adapters.DeviceAdapter
import com.example.smart_home.models.Device
import com.example.smart_home.models.Floor
import com.example.smart_home.utils.AppLogger
import com.example.smart_home.viewmodels.DashboardViewModel

/**
 * Dashboard Fragment - Main screen showing all devices
 */
class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var deviceGrid: RecyclerView
    private lateinit var floorSpinner: Spinner
    private lateinit var devicesOnCount: TextView
    private lateinit var totalDevicesCount: TextView

    private lateinit var deviceAdapter: DeviceAdapter
    private var floors: List<Floor> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AppLogger.d(TAG, "Dashboard fragment created")

        initializeViews(view)
        setupObservers()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        deviceGrid = view.findViewById(R.id.device_grid)
        floorSpinner = view.findViewById(R.id.floor_spinner)
        devicesOnCount = view.findViewById(R.id.devices_on_count)
        totalDevicesCount = view.findViewById(R.id.total_devices_count)

        deviceGrid.layoutManager = GridLayoutManager(requireContext(), 2)

        deviceAdapter = DeviceAdapter(mutableListOf(), { device ->
            toggleDevice(device)
        }, { device ->
            openDeviceDetails(device)
        })
        deviceGrid.adapter = deviceAdapter
    }

    private fun setupObservers() {
        // Observe floors to populate spinner
        viewModel.floors.observe(viewLifecycleOwner) { floorList ->
            floors = floorList
            val floorNames = floors.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floorNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            floorSpinner.adapter = adapter
            
            if (floors.isNotEmpty()) {
                viewModel.selectFloor(floors[0].floorId)
            }
            AppLogger.d(TAG, "Floors loaded: ${floors.size}")
        }

        // Observe devices for current floor
        viewModel.currentFloorDevices.observe(viewLifecycleOwner) { deviceList ->
            deviceAdapter.updateDevices(deviceList)
            updateStats(deviceList)
            AppLogger.d(TAG, "Devices loaded for floor: ${deviceList.size}")
        }

        // Observe all devices to get total count if needed, but currentFloorDevices updateStats might be enough for current view
        viewModel.allDevices.observe(viewLifecycleOwner) { allDevices ->
            totalDevicesCount.text = allDevices.size.toString()
        }

        // Observe active device count
        viewModel.activeDevicesCount.observe(viewLifecycleOwner) { count ->
            devicesOnCount.text = count.toString()
        }

        // Observe errors
        viewModel.getRepositoryError().observe(viewLifecycleOwner) { error ->
            error?.let {
                if (it.isNotEmpty()) {
                    AppLogger.w(TAG, "Error: $it")
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observe success messages
        viewModel.getRepositorySuccess().observe(viewLifecycleOwner) { message ->
            message?.let {
                if (it.isNotEmpty()) {
                    AppLogger.i(TAG, "Success: $it")
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in floors.indices) {
                    val selectedFloor = floors[position]
                    viewModel.selectFloor(selectedFloor.floorId)
                    AppLogger.d(TAG, "Floor selected: ${selectedFloor.name}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun toggleDevice(device: Device) {
        AppLogger.d(TAG, "Toggling device: ${device.name}")
        viewModel.toggleDevice(device)
    }

    private fun openDeviceDetails(device: Device) {
        // Open DeviceControlActivity with deviceId
        val ctx = requireContext()
        val intent = android.content.Intent(ctx, com.example.smart_home.activities.DeviceControlActivity::class.java)
        intent.putExtra("deviceId", device.deviceId)
        startActivity(intent)
    }

    private fun updateStats(devices: List<Device>) {
        // totalDevicesCount is updated by observing allDevices
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppLogger.d(TAG, "Dashboard fragment destroyed")
    }

    companion object {
        private const val TAG = "DashboardFragment"
        fun newInstance() = DashboardFragment()
    }
}
