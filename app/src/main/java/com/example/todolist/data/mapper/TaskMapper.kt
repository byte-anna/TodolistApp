package com.example.todolist.data.mapper

import com.example.todolist.data.local.TaskEntity
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        userId = userId,
        title = title,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt ?: "",
        category = category.name
    )
}

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        userId = userId,
        title = title,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt,
        category = TaskCategory.fromName(category)
    )
}
