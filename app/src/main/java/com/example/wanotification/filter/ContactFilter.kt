package com.example.wanotification.filter

import android.content.Context

import com.example.wanotification.util.NameNormalizer

object ContactFilter {

    fun isAllowed(
        context: Context,
        appPackage: String,
        senderName: String
    ): Boolean {

        val normalized =
            NameNormalizer.normalize(senderName)

        return isAllowedNormalized(
            context,
            appPackage,
            normalized
        )
    }

    fun isAllowedNormalized(
        context: Context,
        appPackage: String,
        normalizedSender: String
    ): Boolean {

        return ContactStore.isAllowedNormalized(
            context,
            appPackage,
            normalizedSender
        )
    }
}