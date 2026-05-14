package com.example.wanotification.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

import com.example.wanotification.cooldown.CooldownManager

class NotificationListener :
    NotificationListenerService() {

    private lateinit var dispatcher:
            NotificationDispatcher

    companion object {
        private const val TAG = "NotificationListener"
    }

    override fun onCreate() {

        super.onCreate()

        // Initialize CooldownManager with context
        CooldownManager.init(this)

        dispatcher =
            NotificationDispatcher(this)

        Log.d(TAG, "Notification listener created")
    }

    override fun onListenerConnected() {

        super.onListenerConnected()

        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        try {
            dispatcher.dispatch(sbn)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch notification", e)
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        CooldownManager.clear()

        Log.d(TAG, "Notification listener destroyed")
    }
}