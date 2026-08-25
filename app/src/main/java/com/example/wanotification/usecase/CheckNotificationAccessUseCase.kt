package com.example.wanotification.usecase

import com.example.wanotification.repository.INotificationRepository

class CheckNotificationAccessUseCase(private val notificationRepository: INotificationRepository) {
    suspend operator fun invoke(): Boolean {
        return notificationRepository.isNotificationListenerEnabled()
    }
}
