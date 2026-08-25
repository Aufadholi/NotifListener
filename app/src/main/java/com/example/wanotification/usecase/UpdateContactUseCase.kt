package com.example.wanotification.usecase

import com.example.wanotification.repository.IContactRepository
import com.example.wanotification.repository.UpdateResult

class UpdateContactUseCase(private val contactRepository: IContactRepository) {
    suspend operator fun invoke(app: String, oldName: String, newName: String): UpdateResult {
        return contactRepository.updateContact(app, oldName, newName)
    }
}
