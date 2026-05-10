package com.example.wanotification.parser

import android.service.notification.StatusBarNotification
import com.example.wanotification.model.ParsedNotification

interface BaseNotificationParser {

    fun parse(
        sbn: StatusBarNotification
    ): ParsedNotification?
}