package com.example.wanotification.parser

import android.service.notification.StatusBarNotification
import com.example.wanotification.model.ParsedNotification

class WhatsAppParser : BaseNotificationParser {

    override fun parse(
        sbn: StatusBarNotification
    ): ParsedNotification? {

        val extras = sbn.notification.extras

        val sender =
            extras.getString("android.title")
                ?: return null

        val bigText =
            extras.getCharSequence("android.bigText")
                ?.toString()

        val text =
            extras.getCharSequence("android.text")
                ?.toString()

        val message = bigText ?: text ?: ""

        return ParsedNotification(

            appPackage = sbn.packageName,

            appName = "WhatsApp",

            senderName = sender,

            message = message,

            timestamp = System.currentTimeMillis()
        )
    }
}