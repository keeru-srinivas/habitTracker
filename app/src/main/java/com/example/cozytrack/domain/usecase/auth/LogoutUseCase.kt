package com.example.cozytrack.domain.usecase.auth

import com.example.cozytrack.domain.repository.AuthRepository

class LogoutUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() {
        repository.logout()
    }
}
