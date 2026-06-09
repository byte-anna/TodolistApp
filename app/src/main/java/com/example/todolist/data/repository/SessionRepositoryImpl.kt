package com.example.todolist.data.repository

import com.example.todolist.data.local.UserPreferences
import com.example.todolist.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class SessionRepositoryImpl(
    private val userPreferences: UserPreferences
) : SessionRepository {

    override val userId: Flow<String?> = userPreferences.userId
    override val userName: Flow<String?> = userPreferences.userName
    override val authToken: Flow<String?> = userPreferences.authToken

    override suspend fun clearSession() {
        userPreferences.clearSession()
    }
}
