package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository
import javax.inject.Inject

class GetEntriesForDayUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(checkDate: String) =
        repository.getEntriesForDay(checkDate)
}
