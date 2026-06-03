package com.example.todolist.domain.repository

import com.example.todolist.domain.model.Task

interface TaskRepository {
    suspend fun getTasks(userId: String): Result<List<Task>>
    suspend fun createTask(
        userId: String,
        title: String,
        priority: Int,
        dueDate: String? = null
    ): Result<Task>
    suspend fun updateTask(taskId: String, userId: String, isDone: Boolean): Result<Boolean>
    suspend fun updateTaskDetails(
        taskId: String,
        userId: String,
        title: String,
        priority: Int,
        dueDate: String?
    ): Result<Boolean>
    suspend fun deleteTask(taskId: String, userId: String): Result<Boolean>
    suspend fun createPost(userId: String, content: String, taskId: String?): Result<Unit>
}