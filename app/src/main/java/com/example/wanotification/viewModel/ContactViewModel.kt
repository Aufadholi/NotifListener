package com.example.wanotification.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wanotification.config.SupportedApps
import com.example.wanotification.repository.AddResult
import com.example.wanotification.repository.UpdateResult
import com.example.wanotification.state.ContactEntry
import com.example.wanotification.state.ContactsUiEvent
import com.example.wanotification.state.ContactsUiState
import com.example.wanotification.usecase.AddContactUseCase
import com.example.wanotification.usecase.DeleteContactUseCase
import com.example.wanotification.usecase.GetContactUseCase
import com.example.wanotification.usecase.UpdateContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val addContactUseCase: AddContactUseCase,
    private val getContactUseCase: GetContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
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

    private fun loadContacts() {
        viewModelScope.launch {
            try {
                val rawContacts = getContactUseCase(_uiState.value.selectedApp)

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
                val result = addContactUseCase(app, name)
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
                deleteContactUseCase(app, name)
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
                val result = updateContactUseCase(app, oldName, newName)
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

data class AppOption(val label: String, val packageName: String)

class ContactsViewModelFactory(
    private val addContactUseCase: AddContactUseCase,
    private val getContactUseCase: GetContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ContactsViewModel(
            addContactUseCase,
            getContactUseCase,
            deleteContactUseCase,
            updateContactUseCase
        ) as T
    }
}
