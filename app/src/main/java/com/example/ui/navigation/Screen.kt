package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Downloads : Screen("downloads")
    object Details : Screen("details/{downloadId}") {
        fun createRoute(downloadId: Long) = "details/$downloadId"
    }
    object Settings : Screen("settings")
    object About : Screen("about")
}
