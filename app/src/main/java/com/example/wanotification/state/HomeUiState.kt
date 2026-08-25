package com.example.wanotification.state

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val notificationAccessGranted: Boolean,
        val ttsEnabled: Boolean
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class HomeUiEvent {
    object CheckNotificationAccess : HomeUiEvent()
    data class ToggleTts(val enabled: Boolean) : HomeUiEvent()
    object OpenNotificationSettings : HomeUiEvent()
}