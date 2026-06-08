package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.repository.HabitRepository

class CreateHabitUseCase(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(title: String, frequency: Frequency, startDate: String) =
        repository.createHabit(
            title = title.trim(),
            frequency = frequency,
            startDate = startDate
        )
}
