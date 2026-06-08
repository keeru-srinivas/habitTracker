package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository

class DeleteHabitUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) =
        repository.deleteHabit(habitId)
}
