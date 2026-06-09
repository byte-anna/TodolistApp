package com.example.todolist.presentation.components.tasks

import com.example.todolist.domain.model.TaskCategory

enum class TaskFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    OVERDUE
}

enum class TaskSortOption {
    PRIORITY,
    DEADLINE,
    CREATED_AT
}

enum class TaskCategoryFilter(val label: String, val category: TaskCategory?) {
    ALL("Все", null),
    STUDY("Учеба", TaskCategory.STUDY),
    WORK("Работа", TaskCategory.WORK),
    HOME("Дом", TaskCategory.HOME),
    PERSONAL("Личное", TaskCategory.PERSONAL)
}
