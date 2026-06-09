package com.example.todolist.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class NotificationSchedulerTest {

    @Test
    fun calculateReminderTimeMillis_returnsOneHourBeforeDeadline() {
        val zoneId = ZoneId.of("Europe/Moscow")
        val clock = Clock.fixed(Instant.parse("2026-06-09T10:00:00Z"), zoneId)

        val reminderTime = NotificationScheduler.calculateReminderTimeMillis(
            dueDateStr = "2026-06-10T15:30:00",
            clock = clock
        )

        val expected = Instant.parse("2026-06-10T11:30:00Z").toEpochMilli()
        assertEquals(expected, reminderTime)
    }

    @Test
    fun calculateReminderTimeMillis_returnsNullForInvalidDate() {
        val reminderTime = NotificationScheduler.calculateReminderTimeMillis("not-a-date")

        assertNull(reminderTime)
    }
}
