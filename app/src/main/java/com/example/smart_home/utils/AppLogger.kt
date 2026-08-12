package com.example.smart_home.utils

import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Application-wide logger that writes to both console and file
 */
object AppLogger {

    private const val TAG = "SmartHome"
    private var logFile: File? = null
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun init(cacheDir: File) {
        logFile = File(cacheDir, "app_logs.txt")
    }

    fun d(tag: String, message: String) {
        Log.d(TAG, "$tag: $message")
        writeToFile("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(TAG, "$tag: $message")
        writeToFile("INFO", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(TAG, "$tag: $message")
        writeToFile("WARN", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "$tag: $message", throwable)
        writeToFile("ERROR", tag, "$message - ${throwable?.message ?: ""}")
    }

    private fun writeToFile(level: String, tag: String, message: String) {
        val file = logFile ?: return

        try {
            BufferedWriter(FileWriter(file, true)).use { writer ->
                val timestamp = sdf.format(Date())
                val logLine = "[$timestamp] $level - $tag: $message\n"
                writer.write(logLine)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }

    fun clearLogs() {
        logFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
    }

    fun getLogs(): String {
        val file = logFile
        if (file == null || !file.exists()) {
            return "No logs available"
        }

        val sb = StringBuilder()
        try {
            file.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    sb.append(line).append("\n")
                }
            }
        } catch (e: IOException) {
            return "Error reading logs: ${e.message}"
        }

        return sb.toString()
    }
}
