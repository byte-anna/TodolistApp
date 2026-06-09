package com.example.todolist.data.repository

import android.util.Log
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.TaskDao
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.data.mapper.toDomain
import com.example.todolist.data.mapper.toEntity
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first

class TaskRepositoryImpl(
    private val api: TodoApi,
    private val taskDao: TaskDao,
    private val userPreferences: UserPreferences
) : TaskRepository {

    companion object {
        private const val TAG = "ROOM_DEBUG"
    }

    override suspend fun getTasks(): Result<List<Task>> {
        val userId = getCurrentUserId()
        return runCatching {
            val cachedCategories = taskDao.getTasksByUserSync(userId)
                .associate { entity -> entity.id to entity.category }

            Log.d(TAG, "Loading tasks from server for current user")
            val tasks = api.getTasks()
            Log.d(TAG, "Server returned ${tasks.size} tasks")

            taskDao.deleteTasksByUser(userId)

            val entities = tasks.map { task ->
                task.copy(category = TaskCategory.fromName(cachedCategories[task.id])).toEntity()
            }
            taskDao.insertAll(entities)

            entities.map { it.toDomain() }
        }.recoverCatching { error ->
            if (error is SessionExpiredException) throw error

            Log.e(TAG, "Network error: ${error.message}. Reading from cache")
            val cached = taskDao.getTasksByUserSync(userId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDomain() }
            } else {
                throw error
            }
        }
    }

    override suspend fun createTask(
        title: String,
        priority: Int,
        dueDate: String?,
        category: TaskCategory
    ): Result<Task> {
        return runCatching {
            val task = api.createTask(title, priority, dueDate).copy(category = category)
            taskDao.insertAll(listOf(task.toEntity()))
            task
        }
    }

    override suspend fun updateTask(taskId: String, isDone: Boolean): Result<Boolean> {
        return runCatching {
            val success = api.updateTask(taskId = taskId, isDone = isDone)
            if (success) {
                taskDao.updateTaskStatus(taskId, isDone)
            }
            success
        }
    }

    override suspend fun updateTaskDetails(
        taskId: String,
        title: String,
        priority: Int,
        dueDate: String?,
        category: TaskCategory
    ): Result<Boolean> {
        return runCatching {
            val success = api.updateTask(
                taskId = taskId,
                title = title,
                priority = priority,
                dueDate = dueDate
            )
            if (success) {
                taskDao.updateTaskDetails(taskId, title, priority, dueDate, category.name)
            }
            success
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Boolean> {
        return runCatching {
            val success = api.deleteTask(taskId)
            if (success) {
                taskDao.deleteTask(taskId)
            }
            success
        }
    }

    override suspend fun createPost(content: String, taskId: String?): Result<Unit> {
        return runCatching {
            api.createPost(content, taskId)
        }
    }

    private suspend fun getCurrentUserId(): String {
        return userPreferences.userId.first().orEmpty()
    }
}
