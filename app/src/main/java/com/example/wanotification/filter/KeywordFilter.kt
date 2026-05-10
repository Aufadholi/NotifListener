package com.example.wanotification.filter

object KeywordFilter {

    private val priorityKeywords = listOf(

        "urgent",

        "darurat",

        "tolong",

        "penting"
    )

    fun containsPriorityKeyword(
        message: String
    ): Boolean {

        return priorityKeywords.any {

            message.lowercase().contains(it)
        }
    }
}