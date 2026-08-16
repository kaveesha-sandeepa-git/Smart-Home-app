package com.example.smart_home.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import com.example.smart_home.R
import com.example.smart_home.fragments.DashboardFragment
import com.example.smart_home.fragments.FloorPlanFragment
import com.example.smart_home.fragments.ReportingFragment
import com.example.smart_home.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fix status bar overlap: let the window draw edge-to-edge,
        // then the toolbar handles its own top inset via fitsSystemWindows on the root layout.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Smart Home"

        bottomNav = findViewById(R.id.bottom_navigation)

        // Load default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .commit()
        }

        // Handle bottom navigation — all tabs load as fragments so the bottom
        // nav bar is always visible regardless of which section is active.
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    supportActionBar?.title = "Smart Home"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment())
                        .commit()
                    true
                }
                R.id.nav_floors -> {
                    supportActionBar?.title = "Floor Plans"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, FloorPlanFragment.newInstance())
                        .commit()
                    true
                }
                R.id.nav_reports -> {
                    supportActionBar?.title = "Usage Reports"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ReportingFragment.newInstance())
                        .commit()
                    true
                }
                R.id.nav_settings -> {
                    supportActionBar?.title = "Settings"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                // TODO: open NotificationsActivity when created
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
