package com.example.todolist.domain.usecase.tasks

import com.example.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, isDone: Boolean): Result<Boolean> {
        return taskRepository.updateTask(taskId, isDone)
    }
}
