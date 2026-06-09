package com.example.todolist.domain.usecase.tasks

import com.example.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Boolean> {
        return taskRepository.deleteTask(taskId)
    }
}
