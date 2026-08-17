package com.example.smart_home.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.smart_home.R
import com.example.smart_home.fragments.DashboardFragment
import com.example.smart_home.fragments.SettingsFragment
import com.example.smart_home.viewmodels.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check authentication
        if (authViewModel.userLiveData.value == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        bottomNav = findViewById(R.id.bottom_navigation)

        // Load default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .commit()
        }

        // Handle bottom navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_floors -> {
                    startActivity(Intent(this, FloorPlanActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportingActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
