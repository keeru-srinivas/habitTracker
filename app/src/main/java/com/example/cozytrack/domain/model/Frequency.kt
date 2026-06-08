package com.example.cozytrack.domain.model

enum class Frequency(val apiValue: String) {
    Daily("daily"),
    Weekly("weekly");

    companion object {
        fun fromApi(value: String): Frequency {
            return entries.firstOrNull { it.apiValue == value.lowercase() } ?: Daily
        }
    }
}
