package com.example.todolist.presentation.components.tasks

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Task
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val mockApi = mockk<TodoApi>(relaxed = true)
    private val mockApp = mockk<Application>(relaxed = true)
    private lateinit var viewModel: TasksViewModel

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TasksViewModel(mockApi, "test_user_123", mockApp)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTasks success - updates state with tasks`() = runTest {
        // GIVEN
        val sampleTasks = listOf(
            Task("1", "test_user_123", "Купить молоко", false, 1, null, null, "2024-01-01T10:00"),
            Task("2", "test_user_123", "Сдать курсовую", true, 3, null, null, "2024-01-02T15:00")
        )
        coEvery { mockApi.getTasks("test_user_123") } returns sampleTasks

        // WHEN
        viewModel.loadTasks()
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        val state = viewModel.uiState.value
        assertEquals(sampleTasks, state.tasks)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `addTask success - calls API and reloads tasks`() = runTest {
        // GIVEN
        val createdTask = Task("3", "test_user_123", "Новая задача", false, 2, null, null, "2024-01-03T09:00")
        coEvery { mockApi.createTask(any(), any(), any(), any(), any()) } returns createdTask
        coEvery { mockApi.getTasks("test_user_123") } returns listOf(createdTask)

        // WHEN
        viewModel.addTask("Новая задача", 2, null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        coVerify { mockApi.createTask("test_user_123", "Новая задача", 2, null, null) }
        assertEquals(listOf(createdTask), viewModel.uiState.value.tasks)
    }

    @Test
    fun `toggleTask - calls API with correct isDone value`() = runTest {
        // GIVEN
        coEvery { mockApi.updateTask(any(), any(), any(), any(), any(), any(), any()) } returns true
        coEvery { mockApi.getTasks("test_user_123") } returns emptyList()

        // WHEN
        viewModel.toggleTask("task_1", true)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        coVerify {
            mockApi.updateTask(
                taskId = "task_1",
                userId = "test_user_123",
                isDone = true,
                folderId = null
            )
        }
    }

    @Test
    fun `loadTasks error - handles API failure gracefully`() = runTest {
        // GIVEN
        coEvery { mockApi.getTasks("test_user_123") } throws RuntimeException("Network error")

        // WHEN
        viewModel.loadTasks()
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        val state = viewModel.uiState.value
        assertEquals(emptyList<Task>(), state.tasks)
        assertEquals(false, state.isLoading)
        assertEquals("Network error", state.error)
    }
}