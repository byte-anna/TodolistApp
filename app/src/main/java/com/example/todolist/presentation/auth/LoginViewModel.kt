package com.example.todolist.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.AuthResponse
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val userName: String? = null,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: TodoApi,
    private val userPreferences: UserPreferences,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.authToken.collect { savedToken ->
                if (!savedToken.isNullOrBlank()) {
                    val savedUserId = userPreferences.userId.first()
                    if (savedUserId != null) {
                        _uiState.value = LoginUiState(isLoggedIn = true, userId = savedUserId)
                    }
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
                val registerResponse = api.register(email, password, displayName)
                val authResponse = if (registerResponse.token.isNullOrBlank()) {
                    api.login(email, password)
                } else {
                    registerResponse
                }
                val userName = saveAuthenticatedSession(authResponse, displayName)
                _uiState.value = LoginUiState(
                    isLoggedIn = true,
                    userId = authResponse.userId,
                    userName = userName
                )
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
                val userName = saveAuthenticatedSession(response)
                _uiState.value = LoginUiState(
                    isLoggedIn = true,
                    userId = response.userId,
                    userName = userName
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

    private suspend fun saveAuthenticatedSession(
        response: AuthResponse,
        fallbackUserName: String? = null
    ): String {
        val token = response.token?.takeIf { it.isNotBlank() }
            ?: error("Сервер не вернул JWT токен")
        userPreferences.saveUserId(response.userId)
        val userName = response.displayName ?: fallbackUserName ?: response.email
        userPreferences.saveUserName(userName)
        userPreferences.saveAuthToken(token)
        return userName
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearSession()
            _uiState.value = LoginUiState()
        }
    }
}