package com.example.todolist.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.presentation.auth.LoginScreen
import com.example.todolist.presentation.auth.LoginViewModel
import com.example.todolist.presentation.components.feed.FeedScreen
import com.example.todolist.presentation.components.tasks.TasksScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val authLoadingMarker = "__auth_loading__"
    val authToken by userPreferences.authToken.collectAsState(initial = authLoadingMarker)
    val userName by userPreferences.userName.collectAsState(initial = null)

    fun navigateToLogin(clearRoute: String) {
        scope.launch {
            userPreferences.clearSession()
            Toast.makeText(context, "Сессия истекла, войдите снова", Toast.LENGTH_LONG).show()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(clearRoute) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.CheckAuth.route
    ) {
        composable(AppRoute.CheckAuth.route) {
            LaunchedEffect(authToken) {
                when {
                    authToken == authLoadingMarker -> Unit
                    authToken.isNullOrBlank() -> {
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(AppRoute.CheckAuth.route) { inclusive = true }
                        }
                    }
                    else -> {
                        navController.navigate(AppRoute.Tasks.route) {
                            popUpTo(AppRoute.CheckAuth.route) { inclusive = true }
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize())
        }

        composable(AppRoute.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { _ ->
                    navController.navigate(AppRoute.Tasks.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.Tasks.route) {
            TasksScreen(
                userName = userName,
                onOpenFeed = { navController.navigate(AppRoute.Feed.route) },
                onLogout = {
                    scope.launch {
                        userPreferences.clearSession()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(AppRoute.Tasks.route) { inclusive = true }
                        }
                    }
                },
                onSessionExpired = {
                    navigateToLogin(AppRoute.Tasks.route)
                }
            )
        }

        composable(AppRoute.Feed.route) {
            FeedScreen(
                onBackClick = { navController.popBackStack() },
                onSessionExpired = {
                    navigateToLogin(AppRoute.Feed.route)
                }
            )
        }
    }
}
