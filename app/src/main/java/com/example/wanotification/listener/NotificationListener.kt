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

    @Volatile
    private var isActive = false

    companion object {
        private const val TAG = "NotificationListener"
    }

    override fun onCreate() {

        super.onCreate()

        isActive = true

        val appContext = applicationContext

        // Initialize CooldownManager with context
        CooldownManager.init(appContext)

        TTSSettingsManager.init(appContext)

        SupportedApps.enabledApps.forEach { pkg ->
            ContactStore.getAllowedContacts(appContext, pkg)
        }

        startForegroundSafely()

        dispatcher =
            NotificationDispatcher(this)

        Log.d(TAG, "Notification listener created")
    }

    override fun onListenerConnected() {

        super.onListenerConnected()

        isActive = true

        startForegroundSafely()

        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {

        super.onListenerDisconnected()

        isActive = false

        try {
            if (::dispatcher.isInitialized) {
                dispatcher.shutdown()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error shutting down dispatcher on disconnect", ex)
        }

        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        if (!isActive) {
            return
        }

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

        isActive = false

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