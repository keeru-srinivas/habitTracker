package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository

class GetServerClockUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke() = repository.getServerClock()
}
