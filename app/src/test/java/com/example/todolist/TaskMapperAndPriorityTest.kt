package com.example.todolist

import com.example.todolist.data.local.TaskEntity
import com.example.todolist.data.mapper.toDomain
import com.example.todolist.data.mapper.toEntity
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMapperAndPriorityTest {

    @Test
    fun taskToEntityMapsIdentityAndTitleFields() {
        val task = task(id = "task-1", userId = "user-1", title = "Купить продукты")

        val entity = task.toEntity()

        assertEquals(task.id, entity.id)
        assertEquals(task.userId, entity.userId)
        assertEquals(task.title, entity.title)
    }

    @Test
    fun taskToEntityMapsCompletionState() {
        val task = task(isDone = true)

        val entity = task.toEntity()

        assertEquals(task.isDone, entity.isDone)
    }

    @Test
    fun taskToEntityMapsPriorityValue() {
        val task = task(priority = TaskPriority.HIGH.value)

        val entity = task.toEntity()

        assertEquals(TaskPriority.HIGH.value, entity.priority)
    }

    @Test
    fun taskToEntityMapsCategoryName() {
        val task = task(category = TaskCategory.WORK)

        val entity = task.toEntity()

        assertEquals(TaskCategory.WORK.name, entity.category)
    }

    @Test
    fun taskToEntityMapsDueDateWhenPresent() {
        val task = task(dueDate = "2026-06-08T10:00:00")

        val entity = task.toEntity()

        assertEquals(task.dueDate, entity.dueDate)
    }

    @Test
    fun taskToEntityUsesEmptyCreatedAtWhenTaskCreatedAtIsNull() {
        val task = task(createdAt = null)

        val entity = task.toEntity()

        assertEquals("", entity.createdAt)
    }

    @Test
    fun taskEntityToDomainMapsIdentityAndTitleFields() {
        val entity = entity(id = "task-2", userId = "user-2", title = "Сделать отчет")

        val task = entity.toDomain()

        assertEquals(entity.id, task.id)
        assertEquals(entity.userId, task.userId)
        assertEquals(entity.title, task.title)
    }

    @Test
    fun taskEntityToDomainMapsStatusPriorityDatesAndCategory() {
        val entity = entity(
            isDone = true,
            priority = TaskPriority.LOW.value,
            dueDate = "2026-06-09T15:00:00",
            createdAt = "2026-06-07T11:30:00",
            category = TaskCategory.STUDY.name
        )

        val task = entity.toDomain()

        assertEquals(entity.isDone, task.isDone)
        assertEquals(entity.priority, task.priority)
        assertEquals(entity.dueDate, task.dueDate)
        assertEquals(entity.createdAt, task.createdAt)
        assertEquals(TaskCategory.STUDY, task.category)
    }

    @Test
    fun taskEntityToDomainKeepsDueDateNullWhenCacheHasNoDueDate() {
        val entity = entity(dueDate = null)

        val task = entity.toDomain()

        assertNull(task.dueDate)
    }

    @Test
    fun taskEntityToDomainKeepsTaskNotSharedByDefault() {
        val entity = entity()

        val task = entity.toDomain()

        assertFalse(task.isShared)
    }

    @Test
    fun taskPriorityFromValueReturnsMatchingPriorityOrMediumForUnknownValue() {
        assertEquals(TaskPriority.LOW, TaskPriority.fromValue(0))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromValue(1))
        assertEquals(TaskPriority.HIGH, TaskPriority.fromValue(2))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromValue(-1))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromValue(99))
    }

    @Test
    fun taskPriorityApiMappingConvertsBetweenClientAndServerValues() {
        assertEquals(1, TaskPriority.toApiValue(TaskPriority.LOW.value))
        assertEquals(2, TaskPriority.toApiValue(TaskPriority.MEDIUM.value))
        assertEquals(3, TaskPriority.toApiValue(TaskPriority.HIGH.value))

        assertEquals(TaskPriority.LOW, TaskPriority.fromApiValue(1))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromApiValue(2))
        assertEquals(TaskPriority.HIGH, TaskPriority.fromApiValue(3))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromApiValue(99))
    }

    private fun task(
        id: String = "task-id",
        userId: String = "user-id",
        title: String = "Название задачи",
        isDone: Boolean = false,
        priority: Int = TaskPriority.MEDIUM.value,
        dueDate: String? = null,
        createdAt: String? = "2026-06-07T09:00:00",
        isShared: Boolean = false,
        category: TaskCategory = TaskCategory.NONE
    ) = Task(
        id = id,
        userId = userId,
        title = title,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt,
        isShared = isShared,
        category = category
    )

    private fun entity(
        id: String = "task-id",
        userId: String = "user-id",
        title: String = "Название задачи",
        isDone: Boolean = false,
        priority: Int = TaskPriority.MEDIUM.value,
        dueDate: String? = null,
        createdAt: String = "2026-06-07T09:00:00",
        category: String = TaskCategory.NONE.name
    ) = TaskEntity(
        id = id,
        userId = userId,
        title = title,
        isDone = isDone,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt,
        category = category
    )
}
