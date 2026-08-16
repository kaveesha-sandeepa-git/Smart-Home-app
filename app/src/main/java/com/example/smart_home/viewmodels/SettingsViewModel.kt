package com.example.smart_home.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.smart_home.repository.SmartHomeRepository

/**
 * ViewModel for Settings screen
 * Handles app configuration and status monitoring
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance(application)
    
    val syncStatus: LiveData<Boolean> = repository.getSyncStatus()
    val lastSyncTime: LiveData<Long> = repository.getLastSyncTime()
    val safetyAlerts: LiveData<String?> = repository.getSafetyAlerts()

    fun refreshFirebaseConnectivity() {
        repository.refreshFirebaseSync()
    }

    // ============= GETTERS =============

    fun getRepositoryError(): LiveData<String> = repository.getRepositoryError()
}
