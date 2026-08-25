package com.example.wanotification.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wanotification.repository.ISettingsRepository
import com.example.wanotification.state.HomeUiEvent
import com.example.wanotification.state.HomeUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HomeViewModel - Manages UI state for Home screen
 *
 * MVVM Architecture:
 * - Repository handles data access (TTSSettingsManager)
 * - ViewModel manages UI state
 * - Side effects (navigation) are emitted as SharedFlow events
 *
 * UDF Pattern:
 * - Events drive all state changes
 * - State is immutable and observable
 * - No direct UI manipulation
 */
class HomeViewModel(
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Side effects (one-time events like navigation)
    private val _sideEffects = MutableSharedFlow<HomeSideEffect>()
    val sideEffects: SharedFlow<HomeSideEffect> = _sideEffects.asSharedFlow()

    init {
        checkNotificationAccess()
    }

    /**
     * Central event handler - all UI interactions route through here
     * This implements the UDF (Unidirectional Data Flow) pattern
     */
    fun handleEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.CheckNotificationAccess -> checkNotificationAccess()
            is HomeUiEvent.ToggleTts -> toggleTts(event.enabled)
            HomeUiEvent.OpenNotificationSettings -> emitOpenNotificationSettings()
        }
    }

    private fun checkNotificationAccess() {
        viewModelScope.launch {
            try {
                val ttsEnabled = settingsRepository.isTtsEnabled()
                val notificationGranted = settingsRepository.isNotificationListenerEnabled()

                _uiState.value = HomeUiState.Success(
                    notificationAccessGranted = notificationGranted,
                    ttsEnabled = ttsEnabled
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun toggleTts(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setTtsEnabled(enabled)
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    _uiState.value = currentState.copy(ttsEnabled = enabled)
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to toggle TTS")
            }
        }
    }

    private fun emitOpenNotificationSettings() {
        viewModelScope.launch {
            _sideEffects.emit(HomeSideEffect.OpenNotificationSettings)
        }
    }
}

/**
 * Side effects for HomeViewModel
 * These are one-time events that need Activity/UI coordination
 */
sealed class HomeSideEffect {
    object OpenNotificationSettings : HomeSideEffect()
}

/**
 * Factory for creating HomeViewModel with dependencies
 * Standard Android ViewModel factory pattern
 */
class HomeViewModelFactory(
    private val settingsRepository: ISettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(settingsRepository) as T
    }
}

