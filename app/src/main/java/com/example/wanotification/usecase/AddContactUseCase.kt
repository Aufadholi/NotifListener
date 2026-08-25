package com.example.wanotification.usecase

import com.example.wanotification.repository.AddResult
import com.example.wanotification.repository.IContactRepository

class AddContactUseCase(private val contactRepository: IContactRepository) {
    suspend operator fun invoke(app: String, name: String): AddResult {
        return contactRepository.addContact(app, name)
    }
}
