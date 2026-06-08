package com.example.cozytrack.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClockResponseDto(
    val utcCalendarDate: String,
    val utcDateTime: String,
    val timezone: String
)
