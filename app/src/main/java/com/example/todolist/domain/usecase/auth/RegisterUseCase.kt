package com.example.todolist.domain.usecase.auth

import com.example.todolist.domain.model.UserSession
import com.example.todolist.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<UserSession> {
        return authRepository.register(email, password, displayName)
    }
}
