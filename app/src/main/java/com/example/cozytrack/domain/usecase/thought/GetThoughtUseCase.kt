package com.example.cozytrack.domain.usecase.thought

import com.example.cozytrack.domain.repository.ThoughtRepository

class GetThoughtUseCase(
    private val repository: ThoughtRepository
) {
    suspend operator fun invoke() = repository.getThought()
}
