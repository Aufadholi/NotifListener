package com.example.wanotification.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wanotification.state.HomeUiEvent
import com.example.wanotification.state.HomeUiState
import com.example.wanotification.usecase.CheckNotificationAccessUseCase
import com.example.wanotification.usecase.EnableTTUseCase
import com.example.wanotification.usecase.GetTtsSettingsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val checkNotificationAccessUseCase: CheckNotificationAccessUseCase,
    private val enableTTUseCase: EnableTTUseCase,
    private val getTtsSettingsUseCase: GetTtsSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<HomeSideEffect>()
    val sideEffects: SharedFlow<HomeSideEffect> = _sideEffects.asSharedFlow()

    init {
        checkNotificationAccess()
    }

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
                val ttsEnabled = getTtsSettingsUseCase()
                val notificationGranted = checkNotificationAccessUseCase()

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
                enableTTUseCase(enabled)
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

sealed class HomeSideEffect {
    object OpenNotificationSettings : HomeSideEffect()
}

class HomeViewModelFactory(
    private val checkNotificationAccessUseCase: CheckNotificationAccessUseCase,
    private val enableTTUseCase: EnableTTUseCase,
    private val getTtsSettingsUseCase: GetTtsSettingsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(
            checkNotificationAccessUseCase,
            enableTTUseCase,
            getTtsSettingsUseCase
        ) as T
    }
}
