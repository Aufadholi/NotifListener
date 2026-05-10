package com.example.wanotification.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener :
    NotificationListenerService() {

    private lateinit var dispatcher:
            NotificationDispatcher

    override fun onCreate() {

        super.onCreate()

        dispatcher =
            NotificationDispatcher(this)
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        dispatcher.dispatch(sbn)
    }
}