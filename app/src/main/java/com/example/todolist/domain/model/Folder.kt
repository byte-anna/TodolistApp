package com.example.todolist.domain.model

// import kotlinx.serialization.Serializable  // ✅ Закомментируй временно
// @Serializable  // ✅ Закомментируй временно

data class Folder(  // ✅ Просто data-класс без аннотации
    val id: String,
    val userId: String,
    val name: String,
    val color: String,
    val createdAt: String
)