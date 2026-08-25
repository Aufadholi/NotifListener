package com.example.wanotification.state

data class SettingsUiState(
    val isTtsEnabled: Boolean = false,
    val isNotificationAccessGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
