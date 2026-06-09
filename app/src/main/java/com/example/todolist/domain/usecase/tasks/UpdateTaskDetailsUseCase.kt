package com.example.todolist.domain.usecase.tasks

import com.example.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskDetailsUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        taskId: String,
        title: String,
        priority: Int,
        dueDate: String?
    ): Result<Boolean> {
        return taskRepository.updateTaskDetails(taskId, title, priority, dueDate)
    }
}
