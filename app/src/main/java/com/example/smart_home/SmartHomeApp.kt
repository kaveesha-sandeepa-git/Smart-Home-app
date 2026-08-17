package com.example.smart_home

import android.app.Application
import com.example.smart_home.utils.AppLogger
import com.example.smart_home.workers.WorkerManager

class SmartHomeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            AppLogger.init(cacheDir)
            AppLogger.i("SmartHomeApp", "Application starting...")
            WorkerManager.schedulePeriodicWork(this)
        } catch (t: Throwable) {
            android.util.Log.e("SmartHomeApp", "Error in onCreate", t)
        }
    }
}
