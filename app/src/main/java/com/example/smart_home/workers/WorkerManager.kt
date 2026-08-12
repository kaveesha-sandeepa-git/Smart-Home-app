package com.example.smart_home.workers

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages all background workers for the app
 * Schedules periodic tasks for sync and safety checks
 */
object WorkerManager {

    private const val TAG = "WorkerManager"
    private const val SYNC_WORK_NAME = "device_sync"
    private const val SAFETY_WORK_NAME = "safety_check"

    fun schedulePeriodicWork(context: Context) {
        Log.d(TAG, "Scheduling periodic background work...")

        // Schedule device sync every 15 minutes
        val syncWorkRequest = PeriodicWorkRequestBuilder<DeviceSyncWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        Log.d(TAG, "Device sync scheduled: every 15 minutes")

        // Schedule safety check every 15 minutes (Minimum periodic interval is 15 mins)
        // Note: The Java snippet suggested 5 minutes, but WorkManager minimum is 15.
        val safetyWorkRequest = PeriodicWorkRequestBuilder<SafetyCheckWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SAFETY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            safetyWorkRequest
        )

        Log.d(TAG, "Safety check scheduled: every 15 minutes")
    }

    fun cancelPeriodicWork(context: Context) {
        Log.d(TAG, "Cancelling periodic background work...")

        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(SAFETY_WORK_NAME)

        Log.d(TAG, "All periodic work cancelled")
    }
}
