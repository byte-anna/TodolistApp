package com.example.todolist.domain.usecase.reminder

import com.example.todolist.domain.service.ReminderScheduler
import javax.inject.Inject

class CancelReminderUseCase @Inject constructor(
    private val reminderScheduler: ReminderScheduler
) {
    operator fun invoke(taskId: String) {
        reminderScheduler.cancel(taskId)
    }
}
