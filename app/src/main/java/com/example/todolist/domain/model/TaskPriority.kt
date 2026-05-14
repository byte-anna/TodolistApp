package com.example.todolist.domain.model

enum class TaskPriority(val value: Int, val label: String) {
    LOW(0, "Низкий"),
    MEDIUM(1, "Средний"),
    HIGH(2, "Высокий");

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: MEDIUM
    }
}