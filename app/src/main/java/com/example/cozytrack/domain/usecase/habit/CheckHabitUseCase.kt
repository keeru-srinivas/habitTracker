package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository

class CheckHabitUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String, completed: Boolean) =
        repository.checkHabit(habitId = habitId, completed = completed)
}
