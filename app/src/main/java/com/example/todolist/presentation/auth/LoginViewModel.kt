package com.example.todolist.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val userName: String? = null,  // ✅ Добавили
    val error: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val api = TodoApi("http://10.0.2.2:8080")
    private val userPreferences = UserPreferences(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.userId.collect { savedUserId ->
                if (savedUserId != null) {
                    _uiState.value = LoginUiState(isLoggedIn = true, userId = savedUserId)
                }
            }
        }
    }

    fun register(email: String, password: String, displayName: String? = null) {
        if (email.isBlank() || password.length < 6) {
            _uiState.value = _uiState.value.copy(
                error = "Email обязателен, пароль минимум 6 символов"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val response = api.register(email, password, displayName)
                userPreferences.saveUserId(response.userId)
                if (displayName != null) {
                    userPreferences.saveUserName(displayName)
                }
                _uiState.value = LoginUiState(isLoggedIn = true, userId = response.userId, userName = displayName)
            } catch (e: Exception) {
                val message = e.message ?: "Ошибка регистрации"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (message.contains("409"))
                        "Пользователь с таким email уже существует"
                    else
                        message
                )
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Заполните email и пароль"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val response = api.login(email, password)
                userPreferences.saveUserId(response.userId)
                if (response.displayName != null) {
                    userPreferences.saveUserName(response.displayName)
                }
                _uiState.value = LoginUiState(
                    isLoggedIn = true,
                    userId = response.userId,
                    userName = response.displayName ?: response.email  // Если имени нет, показываем email
                )
            } catch (e: Exception) {
                val message = e.message ?: "Ошибка входа"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (message.contains("401"))
                        "Неверный email или пароль"
                    else
                        message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserId()
            _uiState.value = LoginUiState()
        }
    }
}