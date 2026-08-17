package com.example.smart_home.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smart_home.R
import com.example.smart_home.models.Floor
import com.example.smart_home.utils.AppLogger
import com.example.smart_home.viewmodels.FloorPlanViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * Floor Plan Fragment - Displays floor layout with devices
 */
class FloorPlanFragment : Fragment() {

    private val viewModel: FloorPlanViewModel by viewModels()

    private lateinit var floorPlanImage: ImageView
    private lateinit var floorSpinner: Spinner
    private lateinit var btnEditFloor: android.widget.ImageButton

    private var floors: List<Floor> = emptyList()
    private var currentFloor: Floor? = null
    
    private var selectedImageUri: Uri? = null
    private var imagePathView: android.widget.TextView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            imagePathView?.text = it.toString()
        }
    }

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
        floorSpinner = view.findViewById(R.id.floor_spinner)
        btnEditFloor = view.findViewById(R.id.btn_edit_floor)

        btnEditFloor.setOnClickListener {
            showEditFloorDialog()
        }

        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < floors.size) {
                    viewModel.selectFloor(floors[position].floorId)
                } else if (position == floors.size) {
                    showAddNewFloorDialog()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun showAddNewFloorDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_floor, null)
        val nameInput = dialogView.findViewById<android.widget.EditText>(R.id.et_floor_name)
        val pickBtn = dialogView.findViewById<android.widget.Button>(R.id.btn_pick_image)
        imagePathView = dialogView.findViewById(R.id.tv_image_path)
        
        pickBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Add New Floor")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString()
                
                if (name.isNotEmpty()) {
                    val floorId = "floor_${System.currentTimeMillis()}"
                    val savedImagePath = selectedImageUri?.let { saveImageToInternal(it, floorId) } ?: ""
                    
                    val floor = Floor(
                        floorId = floorId,
                        name = name,
                        gridWidth = 4, // Default
                        gridHeight = 4, // Default
                        imageUrl = savedImagePath
                    )
                    viewModel.addFloor(floor)
                }
                selectedImageUri = null
            }
            .setNegativeButton("Cancel") { _, _ ->
                selectedImageUri = null
                if (floors.isNotEmpty()) floorSpinner.setSelection(0)
            }
            .show()
    }

    private fun showEditFloorDialog() {
        val floor = currentFloor ?: return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_floor, null)
        val nameInput = dialogView.findViewById<android.widget.EditText>(R.id.et_floor_name)
        val pickBtn = dialogView.findViewById<android.widget.Button>(R.id.btn_pick_image)
        imagePathView = dialogView.findViewById(R.id.tv_image_path)
        
        nameInput.setText(floor.name)
        imagePathView?.text = floor.imageUrl.ifEmpty { "No image selected" }
        
        pickBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Floor")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                floor.name = nameInput.text.toString()
                
                selectedImageUri?.let {
                    val savedPath = saveImageToInternal(it, floor.floorId)
                    floor.imageUrl = savedPath
                }
                
                viewModel.updateFloor(floor)
                selectedImageUri = null
            }
            .setNegativeButton("Cancel") { _, _ -> selectedImageUri = null }
            .show()
    }

    private fun saveImageToInternal(uri: Uri, floorId: String): String {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return ""
            val file = File(requireContext().filesDir, "floor_$floorId.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return file.absolutePath
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save image", e)
            return ""
        }
    }

    private fun observeViewModel() {
        viewModel.floors.observe(viewLifecycleOwner) { floorList ->
            floors = floorList
            val names = floorList.map { it.name }.toMutableList()
            names.add("+ Add New Floor")

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            floorSpinner.adapter = adapter

            if (floorList.isNotEmpty() && currentFloor == null) {
                floorSpinner.setSelection(0)
                viewModel.selectFloor(floorList[0].floorId)
            }
        }

        // Observe current floor
        viewModel.currentFloor.observe(viewLifecycleOwner) { floor ->
            floor?.let {
                currentFloor = it
                
                // Update blueprint image
                if (it.imageUrl.isNotEmpty()) {
                    val file = File(it.imageUrl)
                    if (file.exists()) {
                        floorPlanImage.setImageURI(Uri.fromFile(file))
                        floorPlanImage.alpha = 1.0f
                    }
                } else {
                    floorPlanImage.setImageResource(R.drawable.ic_floor)
                    floorPlanImage.alpha = 0.5f
                }
                AppLogger.d(TAG, "Floor loaded: ${it.name}")
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
