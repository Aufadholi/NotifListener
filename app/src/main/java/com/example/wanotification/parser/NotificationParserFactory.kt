package com.example.wanotification.parser

import com.example.wanotification.config.SupportedApps

object NotificationParserFactory {

    fun getParser(
        packageName: String
    ): BaseNotificationParser? {

        return when (packageName) {

            SupportedApps.WHATSAPP ->
                WhatsAppParser()

            SupportedApps.INSTAGRAM ->
                InstagramParser()

            else -> null
        }
    }
}