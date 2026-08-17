package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smart_home.R
import com.example.smart_home.models.Device
import com.example.smart_home.models.Floor
import com.example.smart_home.utils.AppLogger
import com.example.smart_home.viewmodels.FloorPlanViewModel

/**
 * Floor Plan Fragment - Displays floor layout with devices
 */
class FloorPlanFragment : Fragment() {

    private val viewModel: FloorPlanViewModel by viewModels()

    private lateinit var floorPlanImage: ImageView
    private lateinit var deviceGridOverlay: GridView
    private lateinit var floorSpinner: Spinner

    private var devices: MutableList<Device> = mutableListOf()
    private var floors: List<Floor> = emptyList()
    private var currentFloor: Floor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_floor_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AppLogger.d(TAG, "Floor plan fragment created")

        initializeViews(view)
        observeViewModel()
    }

    private fun initializeViews(view: View) {
        floorPlanImage = view.findViewById(R.id.floor_plan_image)
        deviceGridOverlay = view.findViewById(R.id.device_grid_overlay)
        floorSpinner = view.findViewById(R.id.floor_spinner)

        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in floors.indices) {
                    viewModel.selectFloor(floors[position].floorId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun observeViewModel() {
        viewModel.floors.observe(viewLifecycleOwner) { floorList ->
            floors = floorList
            val names = floorList.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            floorSpinner.adapter = adapter

            if (floorList.isNotEmpty()) {
                val selectedIndex = floorList.indexOfFirst { it.floorId == currentFloor?.floorId }.takeIf { it >= 0 } ?: 0
                floorSpinner.setSelection(selectedIndex, false)
                viewModel.selectFloor(floorList[selectedIndex].floorId)
            }
        }

        // Observe current floor
        viewModel.currentFloor.observe(viewLifecycleOwner) { floor ->
            floor?.let {
                currentFloor = it
                AppLogger.d(TAG, "Floor loaded: ${it.name}")
            }
        }

        // Observe floor devices
        viewModel.floorDevices.observe(viewLifecycleOwner) { deviceList ->
            deviceList?.let {
                devices.clear()
                devices.addAll(it)
                AppLogger.d(TAG, "Devices loaded for floor: ${it.size}")
                // In a real app, you would set an adapter to deviceGridOverlay here
            }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppLogger.d(TAG, "Floor plan fragment destroyed")
    }

    companion object {
        private const val TAG = "FloorPlanFragment"
        fun newInstance() = FloorPlanFragment()
    }
}
