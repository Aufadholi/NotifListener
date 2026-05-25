package com.example.wanotification.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

import com.example.wanotification.config.SupportedApps
import com.example.wanotification.config.TTSSettingsManager
import com.example.wanotification.cooldown.CooldownManager
import com.example.wanotification.filter.AppFilter
import com.example.wanotification.filter.ContactStore
import com.example.wanotification.service.ForegroundNotificationManager

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

        TTSSettingsManager.init(this)

        SupportedApps.enabledApps.forEach { pkg ->
            ContactStore.getAllowedContacts(this, pkg)
        }

        startForegroundSafely()

        dispatcher =
            NotificationDispatcher(this)

        Log.d(TAG, "Notification listener created")
    }

    override fun onListenerConnected() {

        super.onListenerConnected()

        startForegroundSafely()

        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        if (!AppFilter.isAllowed(sbn.packageName)) {
            return
        }

        try {
            dispatcher.dispatch(sbn)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch notification", e)
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        // Attempt to cleanly shutdown the dispatcher and underlying TTS engine
        try {
            if (::dispatcher.isInitialized) {
                dispatcher.shutdown()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error shutting down dispatcher", ex)
        }

        CooldownManager.clear()

        Log.d(TAG, "Notification listener destroyed")
    }

    private fun startForegroundSafely() {
        try {
            val notification =
                ForegroundNotificationManager.createNotificationAndChannel(this)

            startForeground(
                ForegroundNotificationManager.getNotificationId(),
                notification
            )
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to start foreground notification", ex)
        }
    }
}