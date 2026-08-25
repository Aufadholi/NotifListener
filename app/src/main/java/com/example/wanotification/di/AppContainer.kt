package com.example.wanotification.di

import android.content.Context
import com.example.wanotification.repository.*
import com.example.wanotification.usecase.*

interface AppContainer {
    val contactRepository: IContactRepository
    val settingsRepository: ISettingsRepository
    val notificationRepository: INotificationRepository

    val addContactUseCase: AddContactUseCase
    val getContactUseCase: GetContactUseCase
    val deleteContactUseCase: DeleteContactUseCase
    val updateContactUseCase: UpdateContactUseCase
    val checkNotificationAccessUseCase: CheckNotificationAccessUseCase
    val enableTTUseCase: EnableTTUseCase
    val getTtsSettingsUseCase: GetTtsSettingsUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val contactRepository: IContactRepository by lazy {
        ContactRepository(context)
    }

    override val settingsRepository: ISettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val notificationRepository: INotificationRepository by lazy {
        NotificationRepository(context)
    }

    override val addContactUseCase: AddContactUseCase by lazy {
        AddContactUseCase(contactRepository)
    }

    override val getContactUseCase: GetContactUseCase by lazy {
        GetContactUseCase(contactRepository)
    }

    override val deleteContactUseCase: DeleteContactUseCase by lazy {
        DeleteContactUseCase(contactRepository)
    }

    override val updateContactUseCase: UpdateContactUseCase by lazy {
        UpdateContactUseCase(contactRepository)
    }

    override val checkNotificationAccessUseCase: CheckNotificationAccessUseCase by lazy {
        CheckNotificationAccessUseCase(notificationRepository)
    }

    override val enableTTUseCase: EnableTTUseCase by lazy {
        EnableTTUseCase(settingsRepository)
    }

    override val getTtsSettingsUseCase: GetTtsSettingsUseCase by lazy {
        GetTtsSettingsUseCase(settingsRepository)
    }
}
