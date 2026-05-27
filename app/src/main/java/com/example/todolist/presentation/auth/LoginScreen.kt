package com.example.todolist.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todolist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (userId: String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }  // ✅ Добавили
    var isLogin by remember { mutableStateOf(true) }

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoggedIn && uiState.userId != null) {
        LaunchedEffect(Unit) {
            onLoginSuccess(uiState.userId!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(
                id = if (isLogin) R.drawable.login else R.drawable.register
            ),
            contentDescription = if (isLogin) "Вход" else "Регистрация",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Текст без эмодзи
        Text(
            text = if (isLogin) "Вход" else "Регистрация",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Поле Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Логин") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            enabled = !uiState.isLoading
        )

        // ✅ Поле имени (только при регистрации)
        if (!isLogin) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Ваше имя (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isLoading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Поле Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isLogin) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(email, password, displayName.ifBlank { null })
                    }
                }
            ),
            singleLine = true,
            enabled = !uiState.isLoading
        )

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                if (isLogin) {
                    viewModel.login(email, password)
                } else {
                    viewModel.register(email, password, displayName.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading &&
                    email.isNotBlank() &&
                    password.length >= 6
        ) {
            Text(text = if (isLogin) "Войти" else "Зарегистрироваться")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                isLogin = !isLogin
            },
            enabled = !uiState.isLoading
        ) {
            Text(
                text = if (isLogin)
                    "Нет аккаунта? Зарегистрироваться"
                else
                    "Уже есть аккаунт? Войти"
            )
        }
    }
}