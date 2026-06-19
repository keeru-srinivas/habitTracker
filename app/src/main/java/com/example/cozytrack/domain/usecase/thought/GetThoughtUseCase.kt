package com.example.cozytrack.domain.usecase.thought

import com.example.cozytrack.domain.repository.ThoughtRepository
import javax.inject.Inject

class GetThoughtUseCase @Inject constructor(
    private val repository: ThoughtRepository
) {
    suspend operator fun invoke() = repository.getThought()
}
