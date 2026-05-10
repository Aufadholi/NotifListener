package com.example.wanotification.filter

import com.example.wanotification.config.SupportedApps

object AppFilter {

    fun isAllowed(
        packageName: String
    ): Boolean {

        return SupportedApps.enabledApps.contains(
            packageName
        )
    }
}