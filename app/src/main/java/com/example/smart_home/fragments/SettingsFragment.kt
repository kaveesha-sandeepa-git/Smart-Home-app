package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smart_home.BuildConfig
import com.example.smart_home.R
import com.example.smart_home.utils.AppLogger
import com.example.smart_home.utils.PreferencesManager
import com.example.smart_home.viewmodels.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings Fragment - App configuration and status
 */
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var preferencesManager: PreferencesManager

    private lateinit var notificationsToggle: SwitchCompat
    private lateinit var safetyAlertsToggle: SwitchCompat
    private lateinit var autoSyncToggle: SwitchCompat
    private lateinit var darkModeToggle: SwitchCompat
    private lateinit var firebaseStatus: TextView
    private lateinit var lastSyncTime: TextView
    private lateinit var refreshFirebaseButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AppLogger.d(TAG, "Settings fragment created")

        preferencesManager = PreferencesManager.getInstance(requireContext())

        initializeViews(view)
        loadPreferences()
        observeViewModel()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        notificationsToggle = view.findViewById(R.id.notifications_toggle)
        safetyAlertsToggle = view.findViewById(R.id.safety_alerts_toggle)
        autoSyncToggle = view.findViewById(R.id.auto_sync_toggle)
        darkModeToggle = view.findViewById(R.id.dark_mode_toggle)
        firebaseStatus = view.findViewById(R.id.firebase_status)
        lastSyncTime = view.findViewById(R.id.last_sync_time)
        refreshFirebaseButton = view.findViewById(R.id.refresh_firebase_button)

        // Set app version (reads from BuildConfig)
        view.findViewById<TextView>(R.id.tv_app_version)?.text = BuildConfig.VERSION_NAME
    }

    private fun loadPreferences() {
        AppLogger.d(TAG, "Loading preferences")

        notificationsToggle.isChecked = preferencesManager.notificationsEnabled
        safetyAlertsToggle.isChecked = preferencesManager.safetyAlertsEnabled
        autoSyncToggle.isChecked = preferencesManager.autoSyncEnabled
        darkModeToggle.isChecked = preferencesManager.darkModeEnabled

        updateLastSyncDisplay(preferencesManager.lastSyncTime)
    }

    private fun updateLastSyncDisplay(timestamp: Long) {
        lastSyncTime.text = if (timestamp > 0) {
            formatTimestamp(timestamp)
        } else {
            getString(R.string.never_synced)
        }
    }

    private fun observeViewModel() {
        viewModel.syncStatus.observe(viewLifecycleOwner) { isConnected ->
            isConnected?.let {
                if (it) {
                    firebaseStatus.text = getString(R.string.firebase_status_connected)
                    firebaseStatus.setTextColor(resources.getColor(R.color.status_on, null))
                } else {
                    firebaseStatus.text = getString(R.string.firebase_status_disconnected)
                    firebaseStatus.setTextColor(resources.getColor(R.color.status_disconnected, null))
                }
            }
        }

        viewModel.lastSyncTime.observe(viewLifecycleOwner) { timestamp ->
            timestamp?.let { updateLastSyncDisplay(it) }
        }

        viewModel.safetyAlerts.observe(viewLifecycleOwner) { alert ->
            if (!alert.isNullOrEmpty() && safetyAlertsToggle.isChecked) {
                AppLogger.w(TAG, "Safety Alert: $alert")
                android.widget.Toast.makeText(requireContext(), alert, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        refreshFirebaseButton.setOnClickListener {
            refreshFirebaseButton.isEnabled = false
            viewModel.refreshFirebaseConnectivity()
            android.widget.Toast.makeText(
                requireContext(),
                getString(R.string.checking_firebase_connection),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            refreshFirebaseButton.postDelayed({ refreshFirebaseButton.isEnabled = true }, 1500)
        }

        notificationsToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.notificationsEnabled = isChecked
        }

        safetyAlertsToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.safetyAlertsEnabled = isChecked
        }

        autoSyncToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.autoSyncEnabled = isChecked
        }

        darkModeToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.darkModeEnabled = isChecked
            // Apply immediately — no restart needed
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppLogger.d(TAG, "Settings fragment destroyed")
    }

    companion object {
        private const val TAG = "SettingsFragment"
        fun newInstance() = SettingsFragment()
    }
}
