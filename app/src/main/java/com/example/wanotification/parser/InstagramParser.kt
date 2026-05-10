package com.example.wanotification.parser

import android.service.notification.StatusBarNotification

import com.example.wanotification.model.ParsedNotification

class InstagramParser : BaseNotificationParser {

    override fun parse(
        sbn: StatusBarNotification
    ): ParsedNotification? {

        val extras = sbn.notification.extras

        val rawTitle =
            extras.getString("android.title")
                ?: return null

        val text =
            extras.getCharSequence("android.text")
                ?.toString()
                ?: ""

        val sender =
            rawTitle
                .substringAfter(":")
                .trim()

        return ParsedNotification(

            appPackage = sbn.packageName,

            appName = "Instagram",

            senderName = sender,

            message = text,

            timestamp = System.currentTimeMillis()
        )
    }
}