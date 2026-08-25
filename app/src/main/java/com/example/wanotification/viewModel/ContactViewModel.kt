package com.example.wanotification.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wanotification.config.SupportedApps
import com.example.wanotification.uiux.AddResult
import com.example.wanotification.uiux.IContactRepository
import com.example.wanotification.uiux.UpdateResult
import com.example.wanotification.state.ContactEntry
import com.example.wanotification.state.ContactsUiEvent
import com.example.wanotification.state.ContactsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ContactsViewModel - Manages UI state for Contacts screen
 *
 * MVVM Architecture:
 * - Repository handles data access (ContactStore, persistence)
 * - ViewModel manages UI state and business logic
 * - UI observes StateFlow and sends events
 *
 * ⚠️ No business logic changes - just proper state management
 */
class ContactsViewModel(
    private val contactRepository: IContactRepository,
    private val appOptions: List<AppOption> = listOf(
        AppOption("WhatsApp", SupportedApps.WHATSAPP),
        AppOption("Instagram", SupportedApps.INSTAGRAM)
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    /**
     * Central event handler - all UI interactions route through here
     * This implements the UDF (Unidirectional Data Flow) pattern
     */
    fun handleEvent(event: ContactsUiEvent) {
        when (event) {
            ContactsUiEvent.LoadContacts -> loadContacts()
            is ContactsUiEvent.AddContact -> addContact(event.app, event.name)
            is ContactsUiEvent.DeleteContact -> deleteContact(event.app, event.name)
            is ContactsUiEvent.UpdateContact -> updateContact(event.app, event.oldName, event.newName)
            is ContactsUiEvent.SelectApp -> selectApp(event.app)
            is ContactsUiEvent.FilterByApp -> filterByApp(event.app)
        }
    }

    /**
     * Load contacts from repository and convert to ContactEntry for UI
     * Handles: AllowedContacts list + normalized display
     */
    private fun loadContacts() {
        viewModelScope.launch {
            try {
                // Get raw contact names from repository
                val rawContacts = contactRepository.getAllContacts(_uiState.value.selectedApp)

                // Convert to ContactEntry for display
                val contacts = rawContacts.map { name ->
                    val appLabel = appOptions.find {
                        it.packageName == _uiState.value.selectedApp
                    }?.label ?: "Unknown"

                    ContactEntry(
                        name = name,
                        appLabel = appLabel,
                        appPackage = _uiState.value.selectedApp
                    )
                }

                _uiState.value = _uiState.value.copy(
                    contacts = contacts,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load contacts",
                    isLoading = false
                )
            }
        }
    }

    private fun addContact(app: String, name: String) {
        viewModelScope.launch {
            try {
                val result = contactRepository.addContact(app, name)
                when (result) {
                    AddResult.ADDED -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Contact added",
                            inputText = "",
                            error = null
                        )
                        loadContacts()
                    }
                    AddResult.DUPLICATE -> _uiState.value = _uiState.value.copy(
                        error = "Contact already exists",
                        successMessage = null
                    )
                    AddResult.LIMIT -> _uiState.value = _uiState.value.copy(
                        error = "Maximum 5 contacts per app",
                        successMessage = null
                    )
                    AddResult.INVALID -> _uiState.value = _uiState.value.copy(
                        error = "Invalid contact name",
                        successMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to add contact",
                    successMessage = null
                )
            }
        }
    }

    private fun deleteContact(app: String, name: String) {
        viewModelScope.launch {
            try {
                contactRepository.deleteContact(app, name)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Contact deleted",
                    error = null
                )
                loadContacts()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete contact",
                    successMessage = null
                )
            }
        }
    }

    private fun updateContact(app: String, oldName: String, newName: String) {
        viewModelScope.launch {
            try {
                val result = contactRepository.updateContact(app, oldName, newName)
                when (result) {
                    UpdateResult.UPDATED -> {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Contact updated",
                            editingContact = null,
                            inputText = "",
                            error = null
                        )
                        loadContacts()
                    }
                    UpdateResult.DUPLICATE -> _uiState.value = _uiState.value.copy(
                        error = "Contact name already exists",
                        successMessage = null
                    )
                    UpdateResult.INVALID -> _uiState.value = _uiState.value.copy(
                        error = "Invalid contact name",
                        successMessage = null
                    )
                    UpdateResult.NOT_FOUND -> _uiState.value = _uiState.value.copy(
                        error = "Contact not found",
                        successMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update contact",
                    successMessage = null
                )
            }
        }
    }

    private fun selectApp(app: String) {
        _uiState.value = _uiState.value.copy(selectedApp = app)
        loadContacts()
    }

    private fun filterByApp(app: String?) {
        _uiState.value = _uiState.value.copy(selectedFilter = app)
    }
}

/**
 * App option for dropdown selection
 * (Moved from MainActivity for use in ViewModel)
 */
data class AppOption(val label: String, val packageName: String)

/**
 * Factory for creating ContactsViewModel with dependencies
 * Standard Android ViewModel factory pattern
 */
class ContactsViewModelFactory(
    private val contactRepository: IContactRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ContactsViewModel(contactRepository) as T
    }
}

