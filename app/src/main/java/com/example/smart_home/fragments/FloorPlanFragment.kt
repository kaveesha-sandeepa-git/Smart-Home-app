package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
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
    private lateinit var floorName: TextView
    private lateinit var floorInfo: TextView

    private var devices: MutableList<Device> = mutableListOf()
    private var currentFloor: Floor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_floor_plan, container, false)
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
        floorName = view.findViewById(R.id.floor_name)
        floorInfo = view.findViewById(R.id.floor_info)
    }

    private fun observeViewModel() {
        // Observe current floor
        viewModel.currentFloor.observe(viewLifecycleOwner) { floor ->
            floor?.let {
                currentFloor = it
                floorName.text = it.name
                floorInfo.text = "Grid: ${it.gridWidth}x${it.gridHeight}"
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
