package com.example.todolist.domain.repository

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory

interface TaskRepository {
    suspend fun getTasks(): Result<List<Task>>
    suspend fun createTask(
        title: String,
        priority: Int,
        dueDate: String? = null,
        category: TaskCategory = TaskCategory.NONE
    ): Result<Task>
    suspend fun updateTask(taskId: String, isDone: Boolean): Result<Boolean>
    suspend fun updateTaskDetails(
        taskId: String,
        title: String,
        priority: Int,
        dueDate: String?,
        category: TaskCategory
    ): Result<Boolean>
    suspend fun deleteTask(taskId: String): Result<Boolean>
    suspend fun createPost(content: String, taskId: String?): Result<Unit>
}
