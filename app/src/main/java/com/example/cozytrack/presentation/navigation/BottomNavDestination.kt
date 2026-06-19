package com.example.cozytrack.presentation.navigation

enum class BottomNavDestination(
    val icon: String,
    val label: String
) {
    HOME(icon = "🏠", label = "Home"),
    HABITS(icon = "☑", label = "Habits"),
    STATS(icon = "📊", label = "Stats"),
    PROFILE(icon = "🐻", label = "Profile")
}
