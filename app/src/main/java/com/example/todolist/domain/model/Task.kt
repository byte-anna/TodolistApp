package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val userId: String,
    val title: String,
    val isDone: Boolean,
    val priority: Int = 1,
    val dueDate: String?,
    val folderId: String?,
    val createdAt: String
)