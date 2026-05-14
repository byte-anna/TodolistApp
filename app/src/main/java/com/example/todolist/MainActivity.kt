package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.presentation.auth.LoginScreen
import com.example.todolist.presentation.auth.LoginViewModel
import com.example.todolist.presentation.components.tasks.TasksScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // ✅ Используем стандартную тему Material3 (без кастомных цветов)
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val api = remember { TodoApi("http://10.0.2.2:8080") }

    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()

    // Слушаем, есть ли сохраненный пользователь
    val userId by userPreferences.userId.collectAsState(initial = null)

    NavHost(navController = navController, startDestination = "check_auth") {

        // 1. Экран проверки (решаем, куда идти)
        composable("check_auth") {
            LaunchedEffect(userId) {
                if (userId != null) {
                    // Если есть ID -> на задачи
                    navController.navigate("tasks") { popUpTo("check_auth") { inclusive = true } }
                } else {
                    // Если нет -> на вход
                    navController.navigate("login") { popUpTo("check_auth") { inclusive = true } }
                }
            }
            // Пустой экран пока решаем
            Box(modifier = Modifier.fillMaxSize())
        }

        // 2. Экран входа/регистрации
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                onLoginSuccess = { id ->
                    navController.navigate("tasks") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        // 3. Экран задач
        composable("tasks") {
            userId?.let { id ->
                TasksScreen(
                    userId = id,
                    api = api,
                    onLogout = {
                        scope.launch {
                            userPreferences.clearUserId() // Очищаем память
                            navController.navigate("login") { popUpTo("tasks") { inclusive = true } }
                        }
                    }
                )
            }
        }
    }
}