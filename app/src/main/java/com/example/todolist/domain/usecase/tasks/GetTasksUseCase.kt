package com.example.todolist.domain.usecase.tasks

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(): Result<List<Task>> {
        return taskRepository.getTasks()
    }
}
