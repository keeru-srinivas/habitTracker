package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository

class GetHabitsUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(includeArchived: Boolean = false) =
        repository.getHabits(includeArchived = includeArchived)
}
