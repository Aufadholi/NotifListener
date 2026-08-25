package com.example.wanotification.usecase

import com.example.wanotification.repository.ISettingsRepository

class GetTtsSettingsUseCase(private val settingsRepository: ISettingsRepository) {
    suspend operator fun invoke(): Boolean {
        return settingsRepository.isTtsEnabled()
    }
}
