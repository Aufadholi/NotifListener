package com.example.wanotification.di

import android.content.Context
import com.example.wanotification.uiux.ContactRepository
import com.example.wanotification.uiux.IContactRepository
import com.example.wanotification.repository.ISettingsRepository
import com.example.wanotification.repository.SettingsRepository

interface AppContainer {
    val contactRepository: IContactRepository
    val settingsRepository: ISettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val contactRepository: IContactRepository by lazy {
        ContactRepository(context)
    }

    override val settingsRepository: ISettingsRepository by lazy {
        SettingsRepository(context)
    }
}