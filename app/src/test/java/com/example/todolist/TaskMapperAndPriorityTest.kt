package com.example.todolist

import com.example.todolist.data.local.TaskEntity
import com.example.todolist.data.mapper.toDomain
import com.example.todolist.data.mapper.toEntity
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMapperAndPriorityTest {
    @Test
    fun taskToEntity_mapsIdentityAndTitleFields() {
        val task = task(id = "task-1", userId = "user-1", title = "Купить продукты")

        val entity = task.toEntity()

        assertEquals(task.id, entity.id)
        assertEquals(task.userId, entity.userId)
        assertEquals(task.title, entity.title)
    }

    @Test
    fun taskToEntity_mapsCompletionState() {
        val task = task(isDone = true)

        val entity = task.toEntity()

        assertEquals(task.isDone, entity.isDone)
    }

    @Test
    fun taskToEntity_mapsPriorityValue() {
        val task = task(priority = TaskPriority.HIGH.value)

        val entity = task.toEntity()

        assertEquals(TaskPriority.HIGH.value, entity.priority)
    }

    @Test
    fun taskToEntity_mapsDueDateWhenPresent() {
        val task = task(dueDate = "2026-06-08T10:00:00")

        val entity = task.toEntity()

        assertEquals(task.dueDate, entity.dueDate)
    }

    @Test
    fun taskToEntity_usesEmptyCreatedAtWhenTaskCreatedAtIsNull() {
        val task = task(createdAt = null)

        val entity = task.toEntity()

        assertEquals("", entity.createdAt)
    }

    @Test
    fun taskEntityToDomain_mapsIdentityAndTitleFields() {
        val entity = entity(id = "task-2", userId = "user-2", title = "Сделать отчёт")

        val task = entity.toDomain()

        assertEquals(entity.id, task.id)
        assertEquals(entity.userId, task.userId)
        assertEquals(entity.title, task.title)
    }

    @Test
    fun taskEntityToDomain_mapsStatusPriorityAndDates() {
        val entity = entity(
            isDone = true,
            priority = TaskPriority.LOW.value,
            dueDate = "2026-06-09T15:00:00",
            createdAt = "2026-06-07T11:30:00"
        )

        val task = entity.toDomain()

        assertEquals(entity.isDone, task.isDone)
        assertEquals(entity.priority, task.priority)
        assertEquals(entity.dueDate, task.dueDate)
        assertEquals(entity.createdAt, task.createdAt)
    }

    @Test
    fun taskEntityToDomain_keepsDueDateNullWhenCacheHasNoDueDate() {
        val entity = entity(dueDate = null)

        val task = entity.toDomain()

        assertNull(task.dueDate)
    }

    @Test
    fun taskEntityToDomain_keepsTaskNotSharedByDefault() {
        val entity = entity()

        val task = entity.toDomain()

        assertFalse(task.isShared)
    }

    @Test
    fun taskPriorityFromValue_returnsMatchingPriorityOrMediumForUnknownValue() {
        assertEquals(TaskPriority.LOW, TaskPriority.fromValue(0))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromValue(1))
        assertEquals(TaskPriority.HIGH, TaskPriority.fromValue(2))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromValue(-1))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromValue(99))
    }

    private fun task(
        id: String = "task-id",
        userId: String = "user-id",
        title: String = "Название задачи",
        isDone: Boolean = false,
        priority: Int = TaskPriority.MEDIUM.value,
        dueDate: String? = null,
        createdAt: String? = "2026-06-07T09:00:00",
        isShared: Boolean = false
    ) = Task(
        id = id,
        userId = userId,
        title = title,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt,
        isShared = isShared
    )

    private fun entity(
        id: String = "task-id",
        userId: String = "user-id",
        title: String = "Название задачи",
        isDone: Boolean = false,
        priority: Int = TaskPriority.MEDIUM.value,
        dueDate: String? = null,
        createdAt: String = "2026-06-07T09:00:00"
    ) = TaskEntity(
        id = id,
        userId = userId,
        title = title,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt
    )
}