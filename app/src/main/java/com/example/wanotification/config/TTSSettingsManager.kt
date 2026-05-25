package com.example.wanotification.config

import android.content.Context
import android.content.SharedPreferences

object TTSSettingsManager {

    private const val PREFS_NAME = "tts_settings_prefs"
    private const val KEY_TTS_ENABLED = "tts_enabled"
    private const val DEFAULT_ENABLED = true

    @Volatile
    private var cachedEnabled: Boolean? = null

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        ensureLoaded(context)
    }

    fun isEnabled(
        context: Context
    ): Boolean {

        ensureLoaded(context)

        return cachedEnabled ?: DEFAULT_ENABLED
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {

        ensureLoaded(context)

        cachedEnabled = enabled

        prefs?.edit()?.putBoolean(KEY_TTS_ENABLED, enabled)?.apply()
    }

    private fun ensureLoaded(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
        }

        if (cachedEnabled == null) {
            cachedEnabled = prefs?.getBoolean(
                KEY_TTS_ENABLED,
                DEFAULT_ENABLED
            ) ?: DEFAULT_ENABLED
        }
    }
}
