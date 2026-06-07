package com.example.todolist.data.repository

import android.util.Log
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.TaskDao
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.data.mapper.toDomain
import com.example.todolist.data.mapper.toEntity
import com.example.todolist.domain.model.Task
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
            Log.d(TAG, "Получаем задачи с сервера для текущего пользователя")
            val tasks = api.getTasks()
            Log.d(TAG, "Сервер вернул ${tasks.size} задач")

            Log.d(TAG, "Удаляем старые задачи из кэша")
            taskDao.deleteTasksByUser(userId)

            val entities = tasks.map { it.toEntity() }
            Log.d(TAG, "Вставляем ${entities.size} задач в кэш")
            taskDao.insertAll(entities)

            Log.d(TAG, "Кэш обновлён")
            tasks
        }.recoverCatching { error ->
            if (error is SessionExpiredException) throw error
            Log.e(TAG, "Ошибка сети: ${error.message}. Читаем из кэша")
            val cached = taskDao.getTasksByUserSync(userId)
            Log.d(TAG, "В кэше найдено ${cached.size} задач")
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
        dueDate: String?
    ): Result<Task> {
        return runCatching {
            val task = api.createTask(title, priority, dueDate)  // ← ПЕРЕДАЁМ dueDate!
            taskDao.insertAll(listOf(task.toEntity()))
            task
        }
    }

    override suspend fun updateTask(taskId: String, isDone: Boolean): Result<Boolean> {
        return runCatching {
            Log.d(TAG, "️ Обновляем статус задачи $taskId на isDone=$isDone")
            val success = api.updateTask(taskId = taskId, isDone = isDone)
            if (success) {
                taskDao.updateTaskStatus(taskId, isDone)
                Log.d(TAG, "Статус обновлён в кэше")
            }
            success
        }
    }

    override suspend fun updateTaskDetails(
        taskId: String,
        title: String,
        priority: Int,
        dueDate: String?
    ): Result<Boolean> {
        return runCatching {
            Log.d(TAG, "✏Обновляем детали задачи $taskId")
            val success = api.updateTask(
                taskId = taskId,
                title = title,
                priority = priority,
                dueDate = dueDate
            )
            if (success) {
                taskDao.updateTaskDetails(taskId, title, priority, dueDate)
                Log.d(TAG, "Детали обновлены в кэше")
            }
            success
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Boolean> {
        return runCatching {
            Log.d(TAG, "Удаляем задачу $taskId")
            val success = api.deleteTask(taskId)
            if (success) {
                taskDao.deleteTask(taskId)
                Log.d(TAG, "Задача удалена из кэша")
            }
            success
        }
    }

    override suspend fun createPost(content: String, taskId: String?): Result<Unit> {
        return runCatching {
            Log.d(TAG, "📝 Создаём пост в ленте для задачи $taskId")
            api.createPost(content, taskId)
            Log.d(TAG, "✅ Пост создан")
        }
    }

    private suspend fun getCurrentUserId(): String {
        return userPreferences.userId.first().orEmpty()
    }
}