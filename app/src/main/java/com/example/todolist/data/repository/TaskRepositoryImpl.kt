package com.example.todolist.data.repository

import android.util.Log
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.TaskDao
import com.example.todolist.data.mapper.toDomain
import com.example.todolist.data.mapper.toEntity
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository

class TaskRepositoryImpl(
    private val api: TodoApi,
    private val taskDao: TaskDao
) : TaskRepository {

    companion object {
        private const val TAG = "ROOM_DEBUG"
    }

    override suspend fun getTasks(userId: String): Result<List<Task>> {
        return runCatching {
            Log.d(TAG, "🌐 Получаем задачи с сервера для userId=$userId")
            val tasks = api.getTasks(userId)
            Log.d(TAG, "✅ Сервер вернул ${tasks.size} задач")

            Log.d(TAG, "🗑️ Удаляем старые задачи из кэша")
            taskDao.deleteTasksByUser(userId)

            val entities = tasks.map { it.toEntity() }
            Log.d(TAG, "💾 Вставляем ${entities.size} задач в кэш")
            taskDao.insertAll(entities)

            Log.d(TAG, "✅ Кэш обновлён")
            tasks
        }.recoverCatching { error ->
            Log.e(TAG, "❌ Ошибка сети: ${error.message}. Читаем из кэша")
            val cached = taskDao.getTasksByUserSync(userId)
            Log.d(TAG, "📦 В кэше найдено ${cached.size} задач")
            if (cached.isNotEmpty()) {
                cached.map { it.toDomain() }
            } else {
                throw error
            }
        }
    }

    override suspend fun createTask(userId: String, title: String, priority: Int): Result<Task> {
        return runCatching {
            Log.d(TAG, "➕ Создаём задачу: $title")
            val task = api.createTask(userId, title, priority)
            Log.d(TAG, "✅ Задача создана на сервере с id=${task.id}")

            val entity = task.toEntity()
            Log.d(TAG, "💾 Сохраняем в кэш: id=${entity.id}, title=${entity.title}")
            taskDao.insertAll(listOf(entity))
            Log.d(TAG, "✅ Задача сохранена в кэш")
            task
        }.onFailure { error ->
            Log.e(TAG, "❌ Ошибка создания задачи: ${error.message}")
        }
    }

    override suspend fun updateTask(taskId: String, userId: String, isDone: Boolean): Result<Boolean> {
        return runCatching {
            Log.d(TAG, "✏️ Обновляем статус задачи $taskId на isDone=$isDone")
            val success = api.updateTask(taskId = taskId, userId = userId, isDone = isDone)
            if (success) {
                taskDao.updateTaskStatus(taskId, isDone)
                Log.d(TAG, "✅ Статус обновлён в кэше")
            }
            success
        }
    }

    override suspend fun updateTaskDetails(
        taskId: String,
        userId: String,
        title: String,
        priority: Int,
        dueDate: String?,
        folderId: String?
    ): Result<Boolean> {
        return runCatching {
            Log.d(TAG, "✏️ Обновляем детали задачи $taskId")
            val success = api.updateTask(
                taskId = taskId,
                userId = userId,
                title = title,
                priority = priority,
                dueDate = dueDate,
                folderId = folderId
            )
            if (success) {
                taskDao.updateTaskDetails(taskId, title, priority, dueDate, folderId)
                Log.d(TAG, "✅ Детали обновлены в кэше")
            }
            success
        }
    }

    override suspend fun deleteTask(taskId: String, userId: String): Result<Boolean> {
        return runCatching {
            Log.d(TAG, "🗑️ Удаляем задачу $taskId")
            val success = api.deleteTask(taskId, userId)
            if (success) {
                taskDao.deleteTask(taskId)
                Log.d(TAG, "✅ Задача удалена из кэша")
            }
            success
        }
    }
}