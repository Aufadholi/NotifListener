package com.example.wanotification.state

/**
 * DATA class for displaying contact in UI
 * Created from repository data
 */
data class ContactEntry(
    val name: String,
    val appLabel: String,
    val appPackage: String
)

/**
 * UI STATE for Contacts screen - uses UDF pattern
 * All state is immutable and derived from events
 */
data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactEntry> = emptyList(),
    val selectedApp: String = "com.whatsapp",
    val selectedFilter: String? = null,
    val inputText: String = "",
    val editingContact: ContactEntry? = null,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * EVENTS for Contacts screen - drives state changes
 * All user/system interactions are represented as sealed events
 */
sealed class ContactsUiEvent {
    object LoadContacts : ContactsUiEvent()
    data class AddContact(val app: String, val name: String) : ContactsUiEvent()
    data class DeleteContact(val app: String, val name: String) : ContactsUiEvent()
    data class UpdateContact(val app: String, val oldName: String, val newName: String) : ContactsUiEvent()
    data class SelectApp(val app: String) : ContactsUiEvent()
    data class FilterByApp(val app: String?) : ContactsUiEvent()
}

