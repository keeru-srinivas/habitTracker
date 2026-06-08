package com.example.cozytrack.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateHabitRequestDto(
    val title: String,
    val frequency: String,
    val startDate: String,
    val userId: String
)

@Serializable
data class HabitDto(
    val id: String,
    val title: String,
    val frequency: String,
    val startDate: String,
    val userId: String? = null,
    val isArchived: Boolean = false
)

@Serializable
data class HabitUpdateRequestDto(
    val title: String? = null,
    val frequency: String? = null,
    val startDate: String? = null,
    val isArchived: Boolean? = null
)

@Serializable
data class HabitCheckRequestDto(
    val habitId: String,
    val completed: Boolean
)

@Serializable
data class HabitEntryDto(
    val id: String = "",
    val habitId: String,
    val date: String = "",
    val checkDate: String? = null,
    val calendarDate: String? = null,
    val completed: Boolean = false,
    val completedAt: String? = null
)

@Serializable
data class HabitProgressDto(
    val habitId: String,
    val title: String,
    val frequency: String,
    val rangeStart: String,
    val rangeEnd: String,
    val habitStartDate: String,
    val scheduledOpportunities: Int,
    val doneCount: Int,
    val missedCount: Int,
    val skippedCount: Int,
    val completionRate: Double,
    val currentStreak: Int,
    val bestStreak: Int,
    val last14Days: List<ProgressSnapshotDto> = emptyList(),
    val last14Weeks: List<ProgressSnapshotDto> = emptyList()
)

@Serializable
data class ProgressSnapshotDto(
    val calendarDate: String,
    val status: String
)
