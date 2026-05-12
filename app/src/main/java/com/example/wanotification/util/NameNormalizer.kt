package com.example.wanotification.util

import java.util.Locale

object NameNormalizer {

    private val nonNameChars =
        Regex("[^\\p{L}\\p{Nd}]+")

    private val whitespace =
        Regex("\\s+")

    fun normalize(
        input: String
    ): String {

        val trimmed =
            input.trim()

        if (trimmed.isEmpty()) {
            return ""
        }

        val cleaned =
            nonNameChars.replace(trimmed, " ")

        return whitespace
            .replace(cleaned, " ")
            .trim()
            .lowercase(Locale.ROOT)
    }
}
