package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository
import javax.inject.Inject

class GetHabitEntriesUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(
        habitId: String,
        startDate: String? = null,
        endDate: String? = null
    ) = repository.getHabitEntries(
        habitId = habitId,
        startDate = startDate,
        endDate = endDate
    )
}
