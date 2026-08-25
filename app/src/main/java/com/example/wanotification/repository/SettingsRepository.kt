package com.example.wanotification.repository

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.example.wanotification.config.TTSSettingsManager
import com.example.wanotification.listener.NotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ISettingsRepository {
    suspend fun isTtsEnabled(): Boolean
    suspend fun setTtsEnabled(enabled: Boolean)
    suspend fun isNotificationListenerEnabled(): Boolean
}

/**
 * THIN WRAPPER - No business logic changes
 * Delegates all operations to existing managers
 */
class SettingsRepository(private val context: Context) : ISettingsRepository {

    override suspend fun isTtsEnabled(): Boolean =
        withContext(Dispatchers.IO) {
            // Direct delegation to existing TTSSettingsManager - NO CHANGES
            TTSSettingsManager.isEnabled(context)
        }

    override suspend fun setTtsEnabled(enabled: Boolean) =
        withContext(Dispatchers.IO) {
            // Direct delegation to existing TTSSettingsManager - NO CHANGES
            TTSSettingsManager.setEnabled(context, enabled)
        }

    override suspend fun isNotificationListenerEnabled(): Boolean =
        withContext(Dispatchers.Default) {
            // Logic extracted from MainActivity's original isNotificationListenerEnabled()
            // NO business logic changes - exact same implementation
            val expected = ComponentName(context, NotificationListener::class.java).flattenToString()
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            !enabled.isNullOrBlank() && TextUtils.split(enabled, ":").any { it == expected }
        }
}

