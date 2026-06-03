package com.example.todolist.data.mapper

import com.example.todolist.data.local.TaskEntity
import com.example.todolist.domain.model.Task

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        userId = this.userId,
        title = this.title,
        isDone = this.isDone,
        priority = this.priority,
        dueDate = this.dueDate,
        folderId = this.folderId,
        createdAt = this.createdAt ?: ""
    )
}

fun TaskEntity.toDomain(): Task {
    return Task(
        id = this.id,
        userId = this.userId,
        title = this.title,
        isDone = this.isDone,
        priority = this.priority,
        dueDate = this.dueDate,
        folderId = this.folderId,
        createdAt = this.createdAt
    )
}