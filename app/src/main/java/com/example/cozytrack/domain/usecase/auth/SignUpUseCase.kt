package com.example.cozytrack.domain.usecase.auth

import com.example.cozytrack.domain.repository.AuthRepository

class SignUpUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, name: String) =
        repository.signUp(
            email = email.trim(),
            password = password,
            name = name.trim()
        )
}
