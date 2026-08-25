package com.example.wanotification.usecase

import com.example.wanotification.repository.ISettingsRepository

class EnableTTUseCase(private val settingsRepository: ISettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setTtsEnabled(enabled)
    }
}
