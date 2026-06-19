package com.example.cozytrack.domain.usecase.auth

import com.example.cozytrack.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserNameUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.getCurrentUserName()
}
