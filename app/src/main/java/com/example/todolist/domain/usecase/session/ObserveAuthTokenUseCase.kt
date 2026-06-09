package com.example.todolist.domain.usecase.session

import com.example.todolist.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthTokenUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Flow<String?> = sessionRepository.authToken
}
