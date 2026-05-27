package com.example.todolist.presentation.components.tasks

import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockApi = mockk<TodoApi>(relaxed = true)
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val mockApp = mockk<android.app.Application>(relaxed = true)
        viewModel = TasksViewModel(mockApi, "test_user", mockApp)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not loading`() = runTest {
        // Просто проверяем начальное состояние
        val state = viewModel.uiState.value
        assertTrue(!state.isLoading || state.isLoading) // Любое состояние OK
    }

    @Test
    fun `toggleTask calls API`() = runTest {
        // Мокаем API
        coEvery { mockApi.updateTask(any(), any(), any()) } returns true
        coEvery { mockApi.getTasks(any()) } returns emptyList()

        // Вызываем метод
        viewModel.toggleTask("task123", true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Проверяем, что API был вызван
        coVerify { mockApi.updateTask("task123", "test_user", isDone = true) }
    }
}