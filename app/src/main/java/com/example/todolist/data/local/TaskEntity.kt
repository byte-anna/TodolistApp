package com.example.todolist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks_cache")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val isDone: Boolean,
    val priority: Int,
    val dueDate: String?,
    val createdAt: String,
    val category: String = "NONE"
)
