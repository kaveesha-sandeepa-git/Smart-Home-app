package com.example.smart_home

import android.app.Application
import com.example.smart_home.utils.AppLogger
import com.example.smart_home.workers.WorkerManager

class SmartHomeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(cacheDir)
        WorkerManager.schedulePeriodicWork(this)
    }
}
