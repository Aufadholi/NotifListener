package com.example.wanotification.filter

import android.content.Context

import com.example.wanotification.util.NameNormalizer

import org.json.JSONArray

object ContactStore {

    enum class AddResult {
        ADDED,
        DUPLICATE,
        LIMIT,
        INVALID
    }

    private const val PREFS_NAME =
        "contact_filter_prefs"

    private const val KEY_PREFIX =
        "allowed_contacts_"

    private const val MAX_CONTACTS = 5

    private val lock = Any()

    private val cachedRaw =
        mutableMapOf<String, MutableList<String>>()

    private val cachedNormalized =
        mutableMapOf<String, MutableSet<String>>()

    fun isAllowedNormalized(
        context: Context,
        appPackage: String,
        normalizedSender: String
    ): Boolean {

        if (normalizedSender.isBlank()) {
            return false
        }

        val normalizedSet =
            ensureNormalizedSet(context, appPackage)

        if (normalizedSet.isEmpty()) {
            // Strict whitelist mode: if the app has no configured contacts yet,
            // do not allow speech for that app.
            return false
        }

        return normalizedSet.contains(normalizedSender)
    }

    fun getAllowedContacts(
        context: Context,
        appPackage: String
    ): List<String> {

        val rawList =
            ensureRawList(context, appPackage)

        return rawList.toList()
    }

    fun addContact(
        context: Context,
        appPackage: String,
        rawName: String
    ): AddResult {

        val trimmed =
            rawName.trim()

        val normalized =
            NameNormalizer.normalize(trimmed)

        if (normalized.isEmpty()) {
            return AddResult.INVALID
        }

        synchronized(lock) {

            val rawList =
                ensureRawList(context, appPackage)

            val normalizedSet =
                ensureNormalizedSet(context, appPackage)

            if (normalizedSet.contains(normalized)) {
                return AddResult.DUPLICATE
            }

            if (rawList.size >= MAX_CONTACTS) {
                return AddResult.LIMIT
            }

            rawList.add(trimmed)

            normalizedSet.add(normalized)

            save(context, appPackage, rawList)

            return AddResult.ADDED
        }
    }

    fun removeContact(
        context: Context,
        appPackage: String,
        rawName: String
    ): Boolean {

        val normalized =
            NameNormalizer.normalize(rawName)

        synchronized(lock) {

            val rawList =
                ensureRawList(context, appPackage)

            val normalizedSet =
                ensureNormalizedSet(context, appPackage)

            val removed =
                rawList.remove(rawName)

            if (!removed) {

                val index =
                    rawList.indexOfFirst {
                        NameNormalizer.normalize(it) == normalized
                    }

                if (index >= 0) {
                    rawList.removeAt(index)
                } else {
                    return false
                }
            }

            normalizedSet.remove(normalized)

            save(context, appPackage, rawList)

            return true
        }
    }

    fun maxContacts(): Int {
        return MAX_CONTACTS
    }

    private fun ensureRawList(
        context: Context,
        appPackage: String
    ): MutableList<String> {

        synchronized(lock) {

            val existing =
                cachedRaw[appPackage]

            if (existing != null) {
                return existing
            }

            val loaded =
                loadFromPrefs(context, appPackage)

            cachedRaw[appPackage] = loaded

            return loaded
        }
    }

    private fun ensureNormalizedSet(
        context: Context,
        appPackage: String
    ): MutableSet<String> {

        synchronized(lock) {

            val existing =
                cachedNormalized[appPackage]

            if (existing != null) {
                return existing
            }

            val rawList =
                ensureRawList(context, appPackage)

            val loaded =
                cachedNormalized[appPackage]

            if (loaded != null) {
                return loaded
            }

            val normalized =
                rawList.map {
                    NameNormalizer.normalize(it)
                }.filter {
                    it.isNotEmpty()
                }.toMutableSet()

            cachedNormalized[appPackage] = normalized

            return normalized
        }
    }

    private fun loadFromPrefs(
        context: Context,
        appPackage: String
    ): MutableList<String> {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val key =
            KEY_PREFIX + appPackage

        val json =
            prefs.getString(key, null)

        if (json.isNullOrBlank()) {
            return mutableListOf()
        }

        val rawList =
            mutableListOf<String>()

        val normalized =
            mutableSetOf<String>()

        val array = try {
            JSONArray(json)
        } catch (_: Exception) {
            return mutableListOf()
        }

        for (i in 0 until array.length()) {

            val item =
                array.optString(i, "")
                    .trim()

            if (item.isEmpty()) {
                continue
            }

            val normalizedItem =
                NameNormalizer.normalize(item)

            if (normalizedItem.isEmpty()) {
                continue
            }

            if (normalized.contains(normalizedItem)) {
                continue
            }

            rawList.add(item)

            normalized.add(normalizedItem)
        }

        cachedNormalized[appPackage] = normalized

        return rawList
    }

    private fun save(
        context: Context,
        appPackage: String,
        rawList: List<String>
    ) {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val key =
            KEY_PREFIX + appPackage

        val array =
            JSONArray()

        rawList.forEach {
            array.put(it)
        }

        prefs.edit()
            .putString(key, array.toString())
            .apply()
    }
}
