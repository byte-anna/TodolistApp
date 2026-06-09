package com.example.todolist.domain.usecase.tasks

import com.example.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(content: String, taskId: String?): Result<Unit> {
        return taskRepository.createPost(content, taskId)
    }
}
