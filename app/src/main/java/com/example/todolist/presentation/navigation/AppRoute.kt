package com.example.todolist.presentation.navigation

sealed class AppRoute(val route: String) {
    data object CheckAuth : AppRoute("check_auth")
    data object Login : AppRoute("login")
    data object Tasks : AppRoute("tasks")
    data object Feed : AppRoute("feed")
    data object Stats : AppRoute("stats")
}
