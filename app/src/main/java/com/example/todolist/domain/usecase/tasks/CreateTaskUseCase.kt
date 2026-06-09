package com.example.todolist.domain.usecase.tasks

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        title: String,
        priority: Int,
        dueDate: String? = null,
        category: TaskCategory = TaskCategory.NONE
    ): Result<Task> {
        return taskRepository.createTask(title, priority, dueDate, category)
    }
}
