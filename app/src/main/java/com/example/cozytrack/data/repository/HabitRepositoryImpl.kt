package com.example.cozytrack.data.repository

import com.example.cozytrack.core.network.ApiResult
import com.example.cozytrack.core.session.SessionManager
import com.example.cozytrack.data.remote.HabitTrackerApi
import com.example.cozytrack.data.remote.dto.ClockResponseDto
import com.example.cozytrack.data.remote.dto.CreateHabitRequestDto
import com.example.cozytrack.data.remote.dto.HabitCheckRequestDto
import com.example.cozytrack.data.remote.dto.HabitDto
import com.example.cozytrack.data.remote.dto.HabitEntryDto
import com.example.cozytrack.data.remote.dto.HabitProgressDto
import com.example.cozytrack.data.remote.dto.HabitUpdateRequestDto
import com.example.cozytrack.domain.model.Frequency
import com.example.cozytrack.domain.model.Habit
import com.example.cozytrack.domain.model.HabitEntry
import com.example.cozytrack.domain.model.HabitProgress
import com.example.cozytrack.domain.model.ProgressSnapshot
import com.example.cozytrack.domain.model.ServerClock
import com.example.cozytrack.domain.repository.HabitRepository
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class HabitRepositoryImpl @Inject constructor(
    private val api: HabitTrackerApi,
    private val sessionManager: SessionManager
) : HabitRepository {
    override suspend fun getServerClock(): ApiResult<ServerClock> {
        return safeCall { api.getClock().toDomain() }
    }

    override suspend fun getHabits(includeArchived: Boolean): ApiResult<List<Habit>> {
        return safeCall {
            val userId = sessionManager.userId.first()
                ?: error("You must be logged in to load habits")

            api.getHabits(userId = userId, includeArchived = includeArchived)
                .map { it.toDomain() }
        }
    }

    override suspend fun getHabit(habitId: String): ApiResult<Habit> {
        return safeCall {
            api.getHabit(habitId).toDomain()
        }
    }

    override suspend fun createHabit(
        title: String,
        frequency: Frequency,
        startDate: String
    ): ApiResult<String> {
        return safeCall {
            val userId = sessionManager.userId.first()
                ?: error("You must be logged in to create habits")

            api.createHabit(
                CreateHabitRequestDto(
                    title = title,
                    frequency = frequency.apiValue,
                    startDate = startDate,
                    userId = userId
                )
            ).string()
        }
    }

    override suspend fun updateHabit(
        habitId: String,
        title: String?,
        frequency: Frequency?,
        startDate: String?,
        isArchived: Boolean?
    ): ApiResult<Unit> {
        return safeCall {
            api.updateHabit(
                habitId = habitId,
                request = HabitUpdateRequestDto(
                    title = title,
                    frequency = frequency?.apiValue,
                    startDate = startDate,
                    isArchived = isArchived
                )
            ).close()
        }
    }

    override suspend fun deleteHabit(habitId: String): ApiResult<Unit> {
        return safeCall {
            val response = api.deleteHabit(habitId)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                error(
                    if (errorBody.isBlank()) {
                        "HTTP ${response.code()}"
                    } else {
                        "HTTP ${response.code()}: $errorBody"
                    }
                )
            }
        }
    }

    override suspend fun checkHabit(habitId: String, completed: Boolean): ApiResult<Unit> {
        return safeCall {
            api.checkHabit(
                HabitCheckRequestDto(
                    habitId = habitId,
                    completed = completed
                )
            ).close()
        }
    }

    override suspend fun getEntriesForDay(checkDate: String): ApiResult<List<HabitEntry>> {
        return safeCall {
            val userId = sessionManager.userId.first()
                ?: error("You must be logged in to load entries")

            api.getEntriesForDay(userId = userId, checkDate = checkDate)
                .map { it.toDomain() }
        }
    }

    override suspend fun getHabitEntries(
        habitId: String,
        startDate: String?,
        endDate: String?
    ): ApiResult<List<HabitEntry>> {
        return safeCall {
            api.getHabitEntries(
                habitId = habitId,
                startDate = startDate,
                endDate = endDate
            ).map { it.toDomain() }
        }
    }

    override suspend fun getHabitProgress(
        habitId: String,
        startDate: String?,
        endDate: String?
    ): ApiResult<HabitProgress> {
        return safeCall {
            api.getHabitProgress(
                habitId = habitId,
                startDate = startDate,
                endDate = endDate
            ).toDomain()
        }
    }

    private suspend fun <T> safeCall(call: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(call())
        } catch (error: Exception) {
            ApiResult.Error(error.toApiMessage())
        }
    }
}

private fun Exception.toApiMessage(): String {
    if (this is UnknownHostException) {
        return "Cannot reach the CozyTrack server. Check that your emulator/device has internet access and DNS is working."
    }

    if (this is SocketTimeoutException) {
        return "The CozyTrack server took too long to respond. Please try again."
    }

    if (this is HttpException) {
        val errorBody = response()?.errorBody()?.string().orEmpty()
        return if (errorBody.isBlank()) {
            "HTTP ${code()}"
        } else {
            "HTTP ${code()}: $errorBody"
        }
    }

    return message ?: "Something went wrong"
}

private fun ClockResponseDto.toDomain(): ServerClock {
    return ServerClock(
        utcCalendarDate = utcCalendarDate,
        utcDateTime = utcDateTime,
        timezone = timezone
    )
}

private fun HabitDto.toDomain(): Habit {
    return Habit(
        id = id,
        title = title,
        frequency = Frequency.fromApi(frequency),
        startDate = startDate,
        isArchived = isArchived
    )
}

private fun HabitEntryDto.toDomain(): HabitEntry {
    return HabitEntry(
        id = id,
        habitId = habitId,
        date = date.ifBlank { checkDate ?: calendarDate.orEmpty() },
        completed = completed,
        completedAt = completedAt
    )
}

private fun HabitProgressDto.toDomain(): HabitProgress {
    val frequency = Frequency.fromApi(frequency)
    val snapshots = when (frequency) {
        Frequency.Daily -> last14Days
        Frequency.Weekly -> last14Weeks
    }

    return HabitProgress(
        habitId = habitId,
        title = title,
        frequency = frequency,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        scheduledOpportunities = scheduledOpportunities,
        doneCount = doneCount,
        missedCount = missedCount,
        skippedCount = skippedCount,
        completionRate = completionRate,
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        snapshots = snapshots.map {
            ProgressSnapshot(
                calendarDate = it.calendarDate,
                status = it.status
            )
        }
    )
}
