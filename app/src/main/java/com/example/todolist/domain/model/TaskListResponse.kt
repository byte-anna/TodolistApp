package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TaskListResponse(
    val tasks: List<Task>
)