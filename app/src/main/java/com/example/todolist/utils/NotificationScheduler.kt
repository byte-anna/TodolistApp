package com.example.todolist.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object NotificationScheduler {

    fun scheduleReminder(context: Context, taskId: String, title: String, dueDateStr: String?) {
        if (dueDateStr == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Парсим дату и ставим будильник за 1 час до дедлайна
        val dueDateTime = LocalDateTime.parse(dueDateStr)
        val reminderTime = dueDateTime.minusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Если время уже прошло — не ставим
        if (reminderTime > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Можно запросить разрешение через Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                return
            }
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}