package com.example.todolist.domain.repository

import com.example.todolist.domain.model.Post

interface FeedRepository {
    suspend fun getPosts(): Result<List<Post>>
    suspend fun toggleLike(postId: String): Result<Unit>
}
