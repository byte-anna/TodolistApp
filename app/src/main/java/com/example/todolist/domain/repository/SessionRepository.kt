package com.example.todolist.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val userId: Flow<String?>
    val userName: Flow<String?>
    val authToken: Flow<String?>

    suspend fun clearSession()
}
