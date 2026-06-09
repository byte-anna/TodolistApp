package com.example.todolist.data.repository

import com.example.todolist.data.api.AuthResponse
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.domain.model.UserSession
import com.example.todolist.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: TodoApi,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<UserSession> {
        return runCatching {
            val response = api.login(email, password)
            persistSession(response)
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String?
    ): Result<UserSession> {
        return runCatching {
            val registerResponse = api.register(email, password, displayName)
            val authResponse = if (registerResponse.token.isNullOrBlank()) {
                api.login(email, password)
            } else {
                registerResponse
            }
            persistSession(authResponse, displayName)
        }
    }

    private suspend fun persistSession(
        response: AuthResponse,
        fallbackUserName: String? = null
    ): UserSession {
        val token = response.token?.takeIf { it.isNotBlank() }
            ?: error("Сервер не вернул JWT токен")
        val userName = response.displayName ?: fallbackUserName ?: response.email

        userPreferences.saveUserId(response.userId)
        userPreferences.saveUserName(userName)
        userPreferences.saveAuthToken(token)

        return UserSession(
            userId = response.userId,
            email = response.email,
            userName = userName,
            token = token
        )
    }
}
