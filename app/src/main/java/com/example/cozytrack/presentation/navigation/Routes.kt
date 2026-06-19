package com.example.cozytrack.presentation.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val MAIN = "main"
    const val TABS = "tabs"
    const val HABIT_DETAIL = "habit_detail"

    fun habitDetail(habitId: String) = "$HABIT_DETAIL/$habitId"
}
