package com.example.todolist.data.repository

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

    override suspend fun getTasks(userId: String): Result<List<Task>> {
        return runCatching {
            // Сначала пытаемся получить данные с сервера
            val tasks = api.getTasks(userId)

            // При успехе — обновляем локальный кэш
            taskDao.deleteTasksByUser(userId)
            taskDao.insertAll(tasks.map { it.toEntity() })

            tasks
        }.recoverCatching { error ->
            // При ошибке сети — возвращаем данные из кэша
            val cached = taskDao.getTasksByUserSync(userId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDomain() }
            } else {
                throw error // Если кэш пуст — пробрасываем оригинальную ошибку
            }
        }
    }

    override suspend fun createTask(userId: String, title: String, priority: Int): Result<Task> {
        return runCatching {
            val task = api.createTask(userId, title, priority)
            // Добавляем новую задачу в кэш
            taskDao.insertAll(listOf(task.toEntity()))
            task
        }
    }

    override suspend fun updateTask(taskId: String, userId: String, isDone: Boolean): Result<Boolean> {
        return runCatching {
            val success = api.updateTask(taskId = taskId, userId = userId, isDone = isDone)
            if (success) {
                // Обновляем статус в кэше
                taskDao.updateTaskStatus(taskId, isDone)
            }
            success
        }
    }

    override suspend fun deleteTask(taskId: String, userId: String): Result<Boolean> {
        return runCatching {
            val success = api.deleteTask(taskId, userId)
            if (success) {
                // Удаляем из кэша
                taskDao.deleteTask(taskId)
            }
            success
        }
    }
}