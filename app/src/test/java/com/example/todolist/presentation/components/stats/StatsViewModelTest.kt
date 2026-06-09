package com.example.todolist.presentation.components.stats

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskPriority
import com.example.todolist.domain.repository.TaskRepository
import com.example.todolist.domain.usecase.tasks.GetTasksUseCase
import io.mockk.coEvery
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { taskRepository.getTasks() } returns Result.success(emptyList())
        viewModel = StatsViewModel(GetTasksUseCase(taskRepository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadStats calculates summary correctly`() = runTest {
        val tasks = listOf(
            Task("1", "user", "High active", false, TaskPriority.HIGH.value, null, "2026-06-09T10:00:00"),
            Task("2", "user", "Low completed", true, TaskPriority.LOW.value, null, "2026-06-09T11:00:00"),
            Task("3", "user", "Overdue", false, TaskPriority.MEDIUM.value, "2020-01-01T10:00:00", "2026-06-09T12:00:00")
        )
        coEvery { taskRepository.getTasks() } returns Result.success(tasks)

        viewModel.loadStats()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.totalTasks)
        assertEquals(2, state.activeTasks)
        assertEquals(1, state.completedTasks)
        assertEquals(1, state.overdueTasks)
        assertEquals(1, state.highPriorityTasks)
        assertEquals(1, state.mediumPriorityTasks)
        assertEquals(1, state.lowPriorityTasks)
        assertEquals(33, state.completionPercent)
    }

    @Test
    fun `loadStats handles repository error`() = runTest {
        coEvery { taskRepository.getTasks() } returns Result.failure(Exception("Stats error"))

        viewModel.loadStats()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error != null)
    }
}
