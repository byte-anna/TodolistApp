package com.example.todolist.domain.model

data class UserSession(
    val userId: String,
    val email: String,
    val userName: String,
    val token: String
)
