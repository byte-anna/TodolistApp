package com.example.todolist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissions()

        setContent {
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

    private fun requestNotificationPermissions() {
        // Android 13+ (API 33+) — разрешение на уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Уже есть
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Android 12+ (API 32+) — разрешение на точные будильники
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Открываем настройки для запроса разрешения
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
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

    val userId by userPreferences.userId.collectAsState(initial = null)

    NavHost(navController = navController, startDestination = "check_auth") {

        composable("check_auth") {
            LaunchedEffect(userId) {
                if (userId != null) {
                    navController.navigate("tasks") { popUpTo("check_auth") { inclusive = true } }
                } else {
                    navController.navigate("login") { popUpTo("check_auth") { inclusive = true } }
                }
            }
            Box(modifier = Modifier.fillMaxSize())
        }

        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                onLoginSuccess = { id ->
                    navController.navigate("tasks") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("tasks") {
            userId?.let { id ->
                TasksScreen(
                    userId = id,
                    api = api,
                    onLogout = {
                        scope.launch {
                            userPreferences.clearUserId()
                            navController.navigate("login") { popUpTo("tasks") { inclusive = true } }
                        }
                    }
                )
            }
        }
    }
}