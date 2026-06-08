package com.example.cozytrack.domain.model

data class ServerClock(
    val utcCalendarDate: String,
    val utcDateTime: String,
    val timezone: String
)
