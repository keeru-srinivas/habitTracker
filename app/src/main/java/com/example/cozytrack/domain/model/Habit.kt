package com.example.cozytrack.domain.model

data class Habit(
    val id: String,
    val title: String,
    val frequency: Frequency,
    val startDate: String,
    val isArchived: Boolean
)
