package com.example.todolist.presentation.components.calendar

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.model.TaskPriority
import com.example.todolist.domain.repository.TaskRepository
import com.example.todolist.domain.usecase.tasks.GetTasksUseCase
import com.example.todolist.domain.usecase.tasks.UpdateTaskStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<TaskRepository>(relaxed = true)
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getTasks() } returns Result.success(emptyList())
        viewModel = CalendarViewModel(
            getTasksUseCase = GetTasksUseCase(repository),
            updateTaskStatusUseCase = UpdateTaskStatusUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectDate filters tasks by due date`() = runTest {
        val tasks = listOf(
            task(id = "1", title = "Сегодня", dueDate = "2026-06-09T10:00:00"),
            task(id = "2", title = "Завтра", dueDate = "2026-06-10T09:00:00"),
            task(id = "3", title = "Без даты", dueDate = null)
        )
        coEvery { repository.getTasks() } returns Result.success(tasks)

        viewModel.loadTasks()
        viewModel.selectDate(LocalDate.of(2026, 6, 10))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.tasksForSelectedDate.size)
        assertEquals("Завтра", viewModel.uiState.value.tasksForSelectedDate.first().title)
    }

    @Test
    fun `toggleTask updates repository and reloads`() = runTest {
        val tasks = listOf(task(id = "1", title = "Сегодня", dueDate = "2026-06-09T10:00:00"))
        coEvery { repository.getTasks() } returns Result.success(tasks)
        coEvery { repository.updateTask("1", true) } returns Result.success(true)

        viewModel.toggleTask("1", true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateTask("1", true) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun task(
        id: String,
        title: String,
        dueDate: String?
    ) = Task(
        id = id,
        userId = "user",
        title = title,
        isDone = false,
        priority = TaskPriority.MEDIUM.value,
        dueDate = dueDate,
        createdAt = "2026-06-09T08:00:00",
        category = TaskCategory.NONE
    )
}
