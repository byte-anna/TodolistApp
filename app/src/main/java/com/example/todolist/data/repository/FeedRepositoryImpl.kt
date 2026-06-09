package com.example.todolist.data.repository

import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.FeedRepository

class FeedRepositoryImpl(
    private val api: TodoApi
) : FeedRepository {

    override suspend fun getPosts(): Result<List<Post>> {
        return runCatching {
            api.getPosts()
        }
    }

    override suspend fun toggleLike(postId: String): Result<Unit> {
        return runCatching {
            api.toggleLike(postId)
        }
    }
}
