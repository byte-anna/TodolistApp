package com.example.todolist.domain.repository

import com.example.todolist.domain.model.Task

interface TaskRepository {
    suspend fun getTasks(userId: String): Result<List<Task>>
    suspend fun createTask(userId: String, title: String, priority: Int): Result<Task>
    suspend fun updateTask(taskId: String, userId: String, isDone: Boolean): Result<Boolean>
    suspend fun deleteTask(taskId: String, userId: String): Result<Boolean>
}