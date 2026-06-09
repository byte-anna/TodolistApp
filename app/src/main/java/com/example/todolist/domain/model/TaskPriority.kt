package com.example.todolist.domain.model

enum class TaskPriority(
    val value: Int,
    val apiValue: Int,
    val label: String
) {
    LOW(0, 1, "Низкий"),
    MEDIUM(1, 2, "Средний"),
    HIGH(2, 3, "Высокий");

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: MEDIUM
        fun fromApiValue(value: Int) = entries.firstOrNull { it.apiValue == value } ?: MEDIUM
        fun toApiValue(value: Int): Int = fromValue(value).apiValue
    }
}
