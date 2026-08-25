package com.example.wanotification.repository

import android.content.Context
import com.example.wanotification.filter.ContactStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface IContactRepository {
    suspend fun getAllContacts(app: String): List<String>
    suspend fun addContact(app: String, name: String): AddResult
    suspend fun deleteContact(app: String, name: String)
    suspend fun updateContact(app: String, oldName: String, newName: String): UpdateResult
}

class ContactRepository(private val context: Context) : IContactRepository {
    override suspend fun getAllContacts(app: String): List<String> =
        withContext(Dispatchers.IO) {
            ContactStore.getAllowedContacts(context, app)
        }

    override suspend fun addContact(app: String, name: String): AddResult =
        withContext(Dispatchers.IO) {
            when (ContactStore.addContact(context, app, name)) {
                ContactStore.AddResult.ADDED -> AddResult.ADDED
                ContactStore.AddResult.DUPLICATE -> AddResult.DUPLICATE
                ContactStore.AddResult.LIMIT -> AddResult.LIMIT
                ContactStore.AddResult.INVALID -> AddResult.INVALID
            }
        }

    override suspend fun deleteContact(app: String, name: String) {
        withContext(Dispatchers.IO) {
            ContactStore.removeContact(context, app, name)
        }
    }

    override suspend fun updateContact(app: String, oldName: String, newName: String): UpdateResult =
        withContext(Dispatchers.IO) {
            when (ContactStore.updateContact(context, app, oldName, newName)) {
                ContactStore.UpdateResult.UPDATED -> UpdateResult.UPDATED
                ContactStore.UpdateResult.DUPLICATE -> UpdateResult.DUPLICATE
                ContactStore.UpdateResult.INVALID -> UpdateResult.INVALID
                ContactStore.UpdateResult.NOT_FOUND -> UpdateResult.NOT_FOUND
            }
        }
}

enum class AddResult { ADDED, DUPLICATE, LIMIT, INVALID }
enum class UpdateResult { UPDATED, DUPLICATE, INVALID, NOT_FOUND }
