package com.example.wanotification.listener

import android.content.Context
import android.service.notification.StatusBarNotification

import com.example.wanotification.audio.TTSManager
import com.example.wanotification.cooldown.CooldownManager
import com.example.wanotification.filter.AppFilter
import com.example.wanotification.filter.ContactFilter
import com.example.wanotification.parser.NotificationParserFactory
import com.example.wanotification.util.NameNormalizer

class NotificationDispatcher(
    private val context: Context
) {

    private val ttsManager =
        TTSManager(context)

    fun dispatch(
        sbn: StatusBarNotification
    ) {

        // FILTER APP

        if (!AppFilter.isAllowed(sbn.packageName)) {
            return
        }

        // PARSER

        val parser =
            NotificationParserFactory
                .getParser(sbn.packageName)
                ?: return

        val parsed =
            parser.parse(sbn)
                ?: return

        val normalizedSender =
            NameNormalizer.normalize(
                parsed.senderName
            )

        if (normalizedSender.isEmpty()) {
            return
        }

        // FILTER CONTACT

        if (!ContactFilter.isAllowedNormalized(
                context,
                parsed.appPackage,
                normalizedSender
            )
        ) {
            return
        }

        // COOLDOWN

        if (!CooldownManager.canSpeak(normalizedSender)) {
            return
        }

        // SPEECH TEXT

        val speechText =
            "Pesan masuk dari ${parsed.senderName} di ${parsed.appName}."

        // SPEAK

        ttsManager.speak(speechText)
    }
}