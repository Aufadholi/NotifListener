package com.example.wanotification.cooldown

object CooldownManager {

    private const val COOLDOWN_MS = 5000L

    private val lastSpokenMap =
        mutableMapOf<String, Long>()

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
        }

        return allowed
    }
}