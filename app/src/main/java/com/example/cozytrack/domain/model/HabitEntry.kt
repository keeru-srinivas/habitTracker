package com.example.cozytrack.domain.model

data class HabitEntry(
    val id: String,
    val habitId: String,
    val date: String,
    val completed: Boolean,
    val completedAt: String?
)
