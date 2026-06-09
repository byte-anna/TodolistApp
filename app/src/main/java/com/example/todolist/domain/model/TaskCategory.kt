package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TaskCategory(val label: String) {
    NONE("Без категории"),
    STUDY("Учеба"),
    WORK("Работа"),
    HOME("Дом"),
    PERSONAL("Личное");

    companion object {
        fun fromName(value: String?): TaskCategory {
            return entries.firstOrNull { it.name == value } ?: NONE
        }
    }
}
