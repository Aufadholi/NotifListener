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

        val isGroupChat = looksLikeGroupMessage(text)

        val senderName = if (isGroupChat && text != null) {
            text.substringBefore(":").trim().ifBlank { groupTitle }
        } else {
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

    private fun looksLikeGroupMessage(text: String?): Boolean {

        val raw = text?.trim().orEmpty()

        if (raw.isBlank()) return false

        val colonIdx = raw.indexOf(": ")

        if (colonIdx !in 1..40) return false

        val candidate = raw.substring(0, colonIdx).trim()

        if (candidate.isBlank()) return false

        if (candidate.contains("://") || candidate.contains("http")) return false

        return candidate.count { it == ' ' } <= 2
    }
}