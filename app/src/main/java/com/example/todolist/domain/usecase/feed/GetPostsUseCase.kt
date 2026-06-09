package com.example.todolist.domain.usecase.feed

import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.FeedRepository
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(): Result<List<Post>> {
        return feedRepository.getPosts()
    }
}
