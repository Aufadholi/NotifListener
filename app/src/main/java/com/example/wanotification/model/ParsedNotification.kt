package com.example.wanotification.model

data class ParsedNotification(

    val appPackage: String,

    val appName: String,

    val senderName: String,

    val message: String,

    val timestamp: Long,

    val isGroup: Boolean = false,

    val priority: String = "normal"
)