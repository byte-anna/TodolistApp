package com.example.todolist.domain.repository

import com.example.todolist.domain.model.UserSession

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<UserSession>
    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<UserSession>
}
