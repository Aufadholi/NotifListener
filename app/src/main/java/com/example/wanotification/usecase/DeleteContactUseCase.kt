package com.example.wanotification.usecase

import com.example.wanotification.repository.IContactRepository

class DeleteContactUseCase(private val contactRepository: IContactRepository) {
    suspend operator fun invoke(app: String, name: String) {
        contactRepository.deleteContact(app, name)
    }
}
