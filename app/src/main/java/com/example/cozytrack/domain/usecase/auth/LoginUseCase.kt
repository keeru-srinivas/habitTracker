package com.example.cozytrack.domain.usecase.auth

import com.example.cozytrack.domain.repository.AuthRepository

class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        repository.login(email.trim(), password)
}
