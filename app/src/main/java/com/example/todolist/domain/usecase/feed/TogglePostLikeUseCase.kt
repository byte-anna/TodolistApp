package com.example.todolist.domain.usecase.feed

import com.example.todolist.domain.repository.FeedRepository
import javax.inject.Inject

class TogglePostLikeUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String): Result<Unit> {
        return feedRepository.toggleLike(postId)
    }
}
