package com.example.cozytrack.domain.repository

import com.example.cozytrack.core.network.ApiResult
import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.model.Habit
import com.example.cozytrack.domain.model.HabitEntry
import com.example.cozytrack.domain.model.HabitProgress
import com.example.cozytrack.domain.model.ServerClock

interface HabitRepository {
    suspend fun getServerClock(): ApiResult<ServerClock>
    suspend fun getHabits(includeArchived: Boolean = false): ApiResult<List<Habit>>
    suspend fun getHabit(habitId: String): ApiResult<Habit>
    suspend fun createHabit(title: String, frequency: Frequency, startDate: String): ApiResult<String>
    suspend fun updateHabit(
        habitId: String,
        title: String? = null,
        frequency: Frequency? = null,
        startDate: String? = null,
        isArchived: Boolean? = null
    ): ApiResult<Unit>
    suspend fun deleteHabit(habitId: String): ApiResult<Unit>
    suspend fun checkHabit(habitId: String, completed: Boolean): ApiResult<Unit>
    suspend fun getEntriesForDay(checkDate: String): ApiResult<List<HabitEntry>>
    suspend fun getHabitEntries(
        habitId: String,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<List<HabitEntry>>
    suspend fun getHabitProgress(
        habitId: String,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<HabitProgress>
}
