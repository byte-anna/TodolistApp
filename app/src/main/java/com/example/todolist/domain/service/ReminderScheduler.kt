package com.example.todolist.domain.service

interface ReminderScheduler {
    fun schedule(taskId: String, title: String, dueDate: String?)
    fun cancel(taskId: String)
}
