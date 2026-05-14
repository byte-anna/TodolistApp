package com.example.todolist.domain.model  // ✅ Должно быть так!

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val userId: String,
    val content: String,
    val taskId: String? = null,
    val createdAt: String
)