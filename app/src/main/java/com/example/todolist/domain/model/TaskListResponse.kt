package com.example.todolist.domain.model

import kotlinx.serialization.Serializable  // ✅ Импорт!

@Serializable  // ✅ Аннотация!
data class TaskListResponse(
    val tasks: List<Task>
)