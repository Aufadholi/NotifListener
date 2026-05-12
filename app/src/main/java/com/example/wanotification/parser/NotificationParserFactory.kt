package com.example.wanotification.parser

import com.example.wanotification.config.SupportedApps

object NotificationParserFactory {

    private val whatsappParser =
        WhatsAppParser()

    private val instagramParser =
        InstagramParser()

    fun getParser(
        packageName: String
    ): BaseNotificationParser? {

        return when (packageName) {

            SupportedApps.WHATSAPP ->
                whatsappParser

            SupportedApps.INSTAGRAM ->
                instagramParser

            else -> null
        }
    }
}