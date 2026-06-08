package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.repository.HabitRepository

class UpdateHabitUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(
        habitId: String,
        title: String? = null,
        frequency: Frequency? = null,
        startDate: String? = null,
        isArchived: Boolean? = null
    ) = repository.updateHabit(
        habitId = habitId,
        title = title?.trim(),
        frequency = frequency,
        startDate = startDate,
        isArchived = isArchived
    )
}
