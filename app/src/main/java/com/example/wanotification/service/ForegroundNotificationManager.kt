package com.example.wanotification.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wanotification.R

object ForegroundNotificationManager {

    private const val CHANNEL_ID = "wa_notification_channel"
    private const val NOTIFICATION_ID = 1
    private const val CHANNEL_NAME = "WhatsApp Notification Service"

    fun createNotificationAndChannel(
        context: Context
    ): Notification {

        createNotificationChannel(context)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Layanan Notifikasi Aktif")
            .setContentText("Memantau notifikasi WhatsApp...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN
            )

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            manager?.createNotificationChannel(channel)
        }
    }

    fun getNotificationId(): Int = NOTIFICATION_ID
}

