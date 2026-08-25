package com.example.wanotification.usecase

import com.example.wanotification.repository.IContactRepository

class GetContactUseCase(private val contactRepository: IContactRepository) {
    suspend operator fun invoke(app: String): List<String> {
        return contactRepository.getAllContacts(app)
    }
}
