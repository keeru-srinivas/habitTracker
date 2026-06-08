package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository

class GetHabitProgressUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(
        habitId: String,
        startDate: String? = null,
        endDate: String? = null
    ) = repository.getHabitProgress(
        habitId = habitId,
        startDate = startDate,
        endDate = endDate
    )
}
