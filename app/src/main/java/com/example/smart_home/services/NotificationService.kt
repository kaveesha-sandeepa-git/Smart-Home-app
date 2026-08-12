package com.example.smart_home.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smart_home.R
import com.example.smart_home.activities.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service to handle Firebase Cloud Messaging notifications
 */
class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle notification data
        remoteMessage.notification?.let {
            val title = it.title ?: "Smart Home Update"
            val body = it.body ?: ""
            Log.d(TAG, "Notification Title: $title")
            Log.d(TAG, "Notification Body: $body")
            showNotification(title, body, CHANNEL_ID)
        }

        // Handle data message
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val deviceId = remoteMessage.data["deviceId"]
            val message = remoteMessage.data["message"] ?: ""

            Log.d(TAG, "Data Message Type: $type")
            Log.d(TAG, "Device ID: $deviceId")

            when (type) {
                "safety_alert" -> showSafetyAlert(message, deviceId)
                "device_status" -> showDeviceNotification(message, deviceId)
                "schedule_event" -> showScheduleNotification(message, deviceId)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Send the token to your server to store for future messaging
        sendTokenToServer(token)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        createNotificationChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_device)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun showSafetyAlert(message: String, deviceId: String?) {
        Log.w(TAG, "SAFETY ALERT: $message")
        createNotificationChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            deviceId?.let { putExtra("deviceId", it) }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_SAFETY)
            .setSmallIcon(R.drawable.ic_device)
            .setContentTitle("⚠️ SAFETY ALERT")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 250, 500))

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(100, builder.build())
    }

    private fun showDeviceNotification(message: String, deviceId: String?) {
        showNotification("Device Status Update", message, CHANNEL_ID)
    }

    private fun showScheduleNotification(message: String, deviceId: String?) {
        showNotification("Scheduled Event", message, CHANNEL_ID)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // General channel
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Home Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about device status and schedules"
            }
            manager.createNotificationChannel(channel)

            // Safety channel
            val safetyChannel = NotificationChannel(
                CHANNEL_SAFETY,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical safety alerts for high-power devices"
                enableVibration(true)
            }
            manager.createNotificationChannel(safetyChannel)
        }
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Send the FCM token to your backend server
        Log.d(TAG, "TODO: Send FCM token to server: $token")
    }

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "smart_home_notifications"
        private const val CHANNEL_SAFETY = "smart_home_safety"
    }
}
