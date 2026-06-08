package com.example.cozytrack.domain.model

data class HabitProgress(
    val habitId: String,
    val title: String,
    val frequency: Frequency,
    val rangeStart: String,
    val rangeEnd: String,
    val scheduledOpportunities: Int,
    val doneCount: Int,
    val missedCount: Int,
    val skippedCount: Int,
    val completionRate: Double,
    val currentStreak: Int,
    val bestStreak: Int,
    val snapshots: List<ProgressSnapshot>
)

data class ProgressSnapshot(
    val calendarDate: String,
    val status: String
)
