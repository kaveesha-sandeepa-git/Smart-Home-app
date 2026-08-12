package com.example.smart_home.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Centralized error handling and logging system
 */
class ErrorHandler private constructor(private val context: Context) {

    private val errorLiveData = MutableLiveData<ErrorEvent?>()

    data class ErrorEvent(
        val title: String,
        val message: String,
        val level: ErrorLevel,
        val exception: Exception? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class ErrorLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    companion object {
        private const val TAG = "SmartHomeError"
        
        @Volatile
        private var INSTANCE: ErrorHandler? = null

        fun getInstance(context: Context): ErrorHandler {
            return INSTANCE ?: synchronized(this) {
                val instance = ErrorHandler(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // ============= ERROR LOGGING =============

    fun logError(title: String, message: String, e: Exception? = null) {
        logError(title, message, ErrorLevel.ERROR, e)
    }

    fun logError(title: String, message: String, level: ErrorLevel, e: Exception? = null) {
        val event = ErrorEvent(title, message, level, e)
        
        // Log to console and file
        logToConsole(event)
        
        // Post to LiveData for UI observers
        errorLiveData.postValue(event)
        
        // Show user-facing message for errors and critical
        if (level == ErrorLevel.ERROR || level == ErrorLevel.CRITICAL) {
            showUserMessage(message)
        }
    }

    fun logInfo(tag: String, message: String) {
        AppLogger.i(tag, message)
    }

    fun logWarning(tag: String, message: String) {
        AppLogger.w(tag, message)
    }

    fun logDebug(tag: String, message: String) {
        AppLogger.d(tag, message)
    }

    private fun logToConsole(event: ErrorEvent) {
        when (event.level) {
            ErrorLevel.INFO -> AppLogger.i(TAG, "${event.title}: ${event.message}")
            ErrorLevel.WARNING -> AppLogger.w(TAG, "${event.title}: ${event.message}")
            ErrorLevel.ERROR -> AppLogger.e(TAG, "${event.title}: ${event.message}", event.exception)
            ErrorLevel.CRITICAL -> AppLogger.e(TAG, "CRITICAL: ${event.title}: ${event.message}", event.exception)
        }
    }

    private fun showUserMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    // ============= SPECIFIC ERROR HANDLERS =============

    fun handleNetworkError(e: Exception) {
        logError("Network Error", 
                "Unable to connect. Please check your internet connection.", 
                ErrorLevel.WARNING, 
                e)
    }

    fun handleDatabaseError(e: Exception) {
        logError("Database Error", 
                "Failed to access local storage. Please try again.", 
                ErrorLevel.ERROR, 
                e)
    }

    fun handleFirebaseError(e: Exception) {
        logError("Sync Error", 
                "Failed to sync with server. Data will sync when connection is restored.", 
                ErrorLevel.WARNING, 
                e)
    }

    fun handleSafetyError(deviceName: String, e: Exception? = null) {
        logError("Safety Alert", 
                "Critical safety issue detected for $deviceName", 
                ErrorLevel.CRITICAL, 
                e)
    }

    fun handleValidationError(fieldName: String) {
        logError("Validation Error", 
                "$fieldName is invalid. Please check and try again.", 
                ErrorLevel.INFO, 
                null)
    }

    // ============= OBSERVERS =============

    fun getErrorLiveData(): LiveData<ErrorEvent?> = errorLiveData

    fun clearError() {
        errorLiveData.value = null
    }

    // ============= ANALYTICS =============

    fun logCrash(message: String, e: Exception) {
        AppLogger.e("CRASH", message, e)
        // In production, send to Crashlytics or similar service
    }

    fun logEvent(eventName: String, details: String) {
        AppLogger.d("EVENT", "$eventName - $details")
        // In production, send to analytics service
    }
}
