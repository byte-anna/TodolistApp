package com.example.todolist.data.reminder

import android.content.Context
import com.example.todolist.domain.service.ReminderScheduler
import com.example.todolist.utils.NotificationScheduler

class AndroidReminderScheduler(
    private val context: Context
) : ReminderScheduler {

    override fun schedule(taskId: String, title: String, dueDate: String?) {
        NotificationScheduler.scheduleReminder(context, taskId, title, dueDate)
    }

    override fun cancel(taskId: String) {
        NotificationScheduler.cancelReminder(context, taskId)
    }
}
