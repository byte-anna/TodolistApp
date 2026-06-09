package com.example.todolist.domain.usecase.reminder

import com.example.todolist.domain.service.ReminderScheduler
import javax.inject.Inject

class ScheduleReminderUseCase @Inject constructor(
    private val reminderScheduler: ReminderScheduler
) {
    operator fun invoke(taskId: String, title: String, dueDate: String?) {
        reminderScheduler.schedule(taskId, title, dueDate)
    }
}
