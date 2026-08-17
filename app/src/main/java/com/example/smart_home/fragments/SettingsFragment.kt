package com.example.smart_home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smart_home.R
import com.example.smart_home.utils.AppLogger
import com.example.smart_home.utils.PreferencesManager
import com.example.smart_home.viewmodels.AuthViewModel
import com.example.smart_home.viewmodels.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings Fragment - App configuration and status
 */
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var preferencesManager: PreferencesManager

    private lateinit var notificationsToggle: SwitchCompat
    private lateinit var safetyAlertsToggle: SwitchCompat
    private lateinit var autoSyncToggle: SwitchCompat
    private lateinit var darkModeToggle: SwitchCompat
    private lateinit var firebaseStatus: TextView
    private lateinit var lastSyncTime: TextView
    private lateinit var btnLogout: Button

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
        btnLogout = view.findViewById(R.id.btn_logout)
    }

    private fun loadPreferences() {
        AppLogger.d(TAG, "Loading preferences")

        notificationsToggle.isChecked = preferencesManager.notificationsEnabled
        safetyAlertsToggle.isChecked = preferencesManager.safetyAlertsEnabled
        autoSyncToggle.isChecked = preferencesManager.autoSyncEnabled
        darkModeToggle.isChecked = preferencesManager.darkModeEnabled

        val lastSync = preferencesManager.lastSyncTime
        if (lastSync > 0) {
            lastSyncTime.text = "Last sync: ${formatTimestamp(lastSync)}"
        } else {
            lastSyncTime.text = "Never synced"
        }
    }

    private fun observeViewModel() {
        // Observe Firebase connection status
        viewModel.syncStatus.observe(viewLifecycleOwner) { isConnected ->
            isConnected?.let {
                if (it) {
                    firebaseStatus.text = "Connected"
                    firebaseStatus.setTextColor(resources.getColor(R.color.status_on, null))
                } else {
                    firebaseStatus.text = "Disconnected"
                    firebaseStatus.setTextColor(resources.getColor(R.color.status_disconnected, null))
                }
            }
        }

        // Observe safety alerts
        viewModel.safetyAlerts.observe(viewLifecycleOwner) { alert ->
            if (!alert.isNullOrEmpty() && safetyAlertsToggle.isChecked) {
                AppLogger.w(TAG, "Safety Alert: $alert")
                Toast.makeText(requireContext(), alert, Toast.LENGTH_LONG).show()
            }
        }

        authViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                startActivity(android.content.Intent(requireContext(), com.example.smart_home.activities.LoginActivity::class.java))
                requireActivity().finish()
            }
        }
    }

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            authViewModel.logout()
        }

        notificationsToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.notificationsEnabled = isChecked
            AppLogger.d(TAG, "Notifications: ${if (isChecked) "Enabled" else "Disabled"}")
        }

        safetyAlertsToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.safetyAlertsEnabled = isChecked
            AppLogger.d(TAG, "Safety Alerts: ${if (isChecked) "Enabled" else "Disabled"}")
        }

        autoSyncToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.autoSyncEnabled = isChecked
            AppLogger.d(TAG, "Auto Sync: ${if (isChecked) "Enabled" else "Disabled"}")
        }

        darkModeToggle.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.darkModeEnabled = isChecked
            AppLogger.d(TAG, "Dark Mode: ${if (isChecked) "Enabled" else "Disabled"}")
            Toast.makeText(requireContext(), "Restart app to apply theme", Toast.LENGTH_SHORT).show()
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
