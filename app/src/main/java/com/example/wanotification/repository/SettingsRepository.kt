package com.example.wanotification.repository

import android.content.Context
import com.example.wanotification.config.TTSSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ISettingsRepository {
    suspend fun isTtsEnabled(): Boolean
    suspend fun setTtsEnabled(enabled: Boolean)
}

class SettingsRepository(private val context: Context) : ISettingsRepository {

    override suspend fun isTtsEnabled(): Boolean =
        withContext(Dispatchers.IO) {
            TTSSettingsManager.isEnabled(context)
        }

    override suspend fun setTtsEnabled(enabled: Boolean) =
        withContext(Dispatchers.IO) {
            TTSSettingsManager.setEnabled(context, enabled)
        }
}
