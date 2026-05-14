package com.example.wanotification.cooldown

import android.content.Context
import android.content.SharedPreferences

object CooldownManager {

    private const val COOLDOWN_MS = 5000L
    private const val PREFS_NAME = "cooldown_prefs"
    private const val KEY_PREFIX = "cooldown_"

    private val lastSpokenMap =
        mutableMapOf<String, Long>()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        // Load persisted cooldowns
        loadCooldowns()
    }

    private fun loadCooldowns() {
        prefs?.let { pref ->
            val all = pref.all
            for ((key, value) in all) {
                if (key.startsWith(KEY_PREFIX)) {
                    val sender = key.substring(KEY_PREFIX.length)
                    val timestamp = (value as? Long) ?: 0L
                    lastSpokenMap[sender] = timestamp
                }
            }
        }
    }

    fun canSpeak(
        sender: String
    ): Boolean {

        val now = System.currentTimeMillis()

        val lastTime =
            lastSpokenMap[sender] ?: 0L

        val allowed =
            now - lastTime > COOLDOWN_MS

        if (allowed) {

            lastSpokenMap[sender] = now

            // Persist to SharedPreferences
            prefs?.edit()?.apply {
                putLong(KEY_PREFIX + sender, now)
                apply()
            }
        }

        return allowed
    }

    fun clear() {
        lastSpokenMap.clear()
        prefs?.edit()?.clear()?.apply()
    }
}