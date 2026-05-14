package com.example.wanotification.parser

import android.service.notification.StatusBarNotification
import com.example.wanotification.model.ParsedNotification

class WhatsAppParser : BaseNotificationParser {

    override fun parse(
        sbn: StatusBarNotification
    ): ParsedNotification? {

        val extras = sbn.notification.extras

        val groupTitle =
            extras.getString("android.title")
                ?: return null

        val bigText =
            extras.getCharSequence("android.bigText")
                ?.toString()

        val text =
            extras.getCharSequence("android.text")
                ?.toString()

        val message = bigText ?: text ?: ""

        // Detect group chat: if text contains ":", format is "Nama Pengirim: isi pesan"
        val isGroupChat = text?.contains(":") ?: false

        val senderName = if (isGroupChat && text != null) {
            // Extract actual sender name from "Sender: Message" format
            text.substringBefore(":").trim()
        } else {
            // Direct message, sender is the title
            groupTitle
        }

        return ParsedNotification(

            appPackage = sbn.packageName,

            appName = "WhatsApp",

            senderName = senderName,

            message = message,

            timestamp = System.currentTimeMillis(),

            isGroup = isGroupChat
        )
    }
}