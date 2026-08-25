package com.example.wanotification.repository

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.example.wanotification.listener.NotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface INotificationRepository {
    suspend fun isNotificationListenerEnabled(): Boolean
}

class NotificationRepository(private val context: Context) : INotificationRepository {
    override suspend fun isNotificationListenerEnabled(): Boolean =
        withContext(Dispatchers.Default) {
            val expected = ComponentName(context, NotificationListener::class.java).flattenToString()
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            !enabled.isNullOrBlank() && TextUtils.split(enabled, ":").any { it == expected }
        }
}
