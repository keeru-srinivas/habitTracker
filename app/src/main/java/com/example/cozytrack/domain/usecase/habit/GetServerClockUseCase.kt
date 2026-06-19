package com.example.cozytrack.domain.usecase.habit

import com.example.cozytrack.domain.repository.HabitRepository
import javax.inject.Inject

class GetServerClockUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke() = repository.getServerClock()
}
