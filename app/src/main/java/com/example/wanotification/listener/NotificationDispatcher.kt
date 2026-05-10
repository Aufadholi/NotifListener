package com.example.wanotification.listener

import android.content.Context
import android.service.notification.StatusBarNotification

import com.example.wanotification.audio.TTSManager
import com.example.wanotification.cooldown.CooldownManager
import com.example.wanotification.filter.AppFilter
import com.example.wanotification.filter.ContactFilter
import com.example.wanotification.filter.KeywordFilter
import com.example.wanotification.parser.NotificationParserFactory

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

        // FILTER CONTACT

        if (!ContactFilter.isAllowed(parsed.senderName)) {
            return
        }

        // KEYWORD PRIORITY

        val isPriority =
            KeywordFilter.containsPriorityKeyword(
                parsed.message
            )

        // COOLDOWN

        if (!isPriority &&
            !CooldownManager.canSpeak(
                parsed.senderName
            )
        ) {
            return
        }

        // SPEECH TEXT

        val speechText = if (isPriority) {

            "Pesan penting dari ${parsed.senderName}, ${parsed.appName}. Isi pesan, ${parsed.message}"

        } else {

            "Pesan masuk dari ${parsed.senderName}, ${parsed.appName}. Isi pesan, ${parsed.message}"
        }

        // SPEAK

        ttsManager.speak(speechText)
    }
}