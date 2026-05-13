package com.example.wanotification.config

import android.content.Context

object TTSSettingsManager {

    private const val PREFS_NAME = "tts_settings_prefs"
    private const val KEY_TTS_ENABLED = "tts_enabled"
    private const val DEFAULT_ENABLED = false

    fun isEnabled(
        context: Context
    ): Boolean {

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return prefs.getBoolean(
            KEY_TTS_ENABLED,
            DEFAULT_ENABLED
        )
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit().apply {
            putBoolean(KEY_TTS_ENABLED, enabled)
            apply()
        }
    }
}
