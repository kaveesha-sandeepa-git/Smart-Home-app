package com.example.smart_home.activities

import android.os.Bundle
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smart_home.R
import com.example.smart_home.models.Floor

class FloorPlanActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var floorPlanImage: ImageView
    private lateinit var deviceGridOverlay: GridView
    private lateinit var floorSpinner: android.widget.Spinner
    private lateinit var currentFloor: Floor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor_plan)

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        floorPlanImage = findViewById(R.id.floor_plan_image)
        deviceGridOverlay = findViewById(R.id.device_grid_overlay)
        floorSpinner = findViewById(R.id.floor_spinner)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Floor Plans"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load sample floor
        loadSampleFloor()
    }

    private fun loadSampleFloor() {
        currentFloor = Floor("floor1", "Ground Floor", "Main level", "", 4, 4)
        floorSpinner.adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(currentFloor.name)
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // TODO: Set floor plan image
        // floorPlanImage.setImageResource(R.drawable.floor_plan_1)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
