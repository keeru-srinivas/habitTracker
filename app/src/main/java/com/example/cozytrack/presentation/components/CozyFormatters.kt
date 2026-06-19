package com.example.cozytrack.presentation.components

import java.text.SimpleDateFormat
import java.util.Locale

fun String.prettyDate(): String {
    if (isBlank()) return "Loading..."

    return runCatching {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val parsedDate = input.parse(this) ?: return@runCatching this
        output.format(parsedDate)
    }.getOrDefault(this)
}

fun String.shortDateLabel(): String {
    if (isBlank()) return ""

    return runCatching {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("MMM d", Locale.US)
        val parsedDate = input.parse(this) ?: return@runCatching this
        output.format(parsedDate)
    }.getOrDefault(this)
}
