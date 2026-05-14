package com.example.todolist.data.repository

import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository

class TaskRepositoryImpl(
    private val api: TodoApi
) : TaskRepository {

    override suspend fun getTasks(userId: String): Result<List<Task>> {
        return runCatching { api.getTasks(userId) }
    }

    override suspend fun createTask(userId: String, title: String, priority: Int): Result<Task> {
        return runCatching { api.createTask(userId, title, priority) }
    }

    override suspend fun updateTask(taskId: String, userId: String, isDone: Boolean): Result<Boolean> {
        return runCatching {
            // ✅ Явно указываем, что это параметр isDone
            api.updateTask(
                taskId = taskId,
                userId = userId,
                isDone = isDone  // ← Именованный параметр!
            )
        }
    }

    override suspend fun deleteTask(taskId: String, userId: String): Result<Boolean> {
        return runCatching { api.deleteTask(taskId, userId) }
    }
}