package com.example.todolist.presentation.components.tasks

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository
import com.example.todolist.utils.NotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository = mockk<TaskRepository>(relaxed = true)
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Мокаем object NotificationScheduler
        mockkObject(NotificationScheduler)
        every { NotificationScheduler.cancelReminder(any(), any()) } returns Unit
        every { NotificationScheduler.scheduleReminder(any(), any(), any(), any()) } returns Unit

        val mockApp = mockk<android.app.Application>(relaxed = true)

        coEvery { mockRepository.getTasks(any()) } returns Result.success(emptyList())

        viewModel = TasksViewModel(mockRepository, "test_user", mockApp)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(NotificationScheduler)
    }

    @Test
    fun `initial state has empty tasks`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.tasks.isEmpty())
    }

    @Test
    fun `toggleTask calls updateTask repository with correct parameters`() = runTest {
        coEvery { mockRepository.updateTask(any(), any(), any()) } returns Result.success(true)

        viewModel.toggleTask("task123", true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.updateTask("task123", "test_user", true) }
    }

    @Test
    fun `addTask calls repository and reloads tasks`() = runTest {
        val mockTask = Task("1", "test_user", "Новая задача", false, 2, null, null, "")
        coEvery { mockRepository.createTask(any(), any(), any()) } returns Result.success(mockTask)

        viewModel.addTask("Новая задача", 2, null, false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mockRepository.createTask("test_user", "Новая задача", 2)
        }
    }

    @Test
    fun `deleteTask calls deleteTask repository`() = runTest {
        coEvery { mockRepository.deleteTask(any(), any()) } returns Result.success(true)

        viewModel.deleteTask("task456")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.deleteTask("task456", "test_user") }
    }

    @Test
    fun `loadTasks updates state with fetched tasks`() = runTest {
        val mockTasks = listOf(
            Task("1", "test_user", "Задача 1", false, 1, null, null, ""),
            Task("2", "test_user", "Задача 2", true, 2, null, null, "")
        )
        coEvery { mockRepository.getTasks("test_user") } returns Result.success(mockTasks)

        viewModel.loadTasks()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.tasks.size)
        assertEquals("Задача 1", state.tasks[0].title)
    }

    @Test
    fun `loadTasks handles repository error gracefully`() = runTest {
        coEvery { mockRepository.getTasks("test_user") } returns Result.failure(Exception("Network error"))

        viewModel.loadTasks()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error != null)
    }

    @Test
    fun `showAddDialog sets dialogTask in state`() = runTest {
        viewModel.showAddDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.dialogTask != null)
        assertEquals("", state.dialogTask!!.id)
    }

    @Test
    fun `updateTask calls updateTaskDetails with correct parameters`() = runTest {
        val task = Task("task789", "test_user", "Старая задача", false, 1, null, null, "")
        viewModel.showEditDialog(task)

        coEvery { mockRepository.updateTaskDetails(any(), any(), any(), any(), any(), any()) } returns Result.success(true)

        viewModel.updateTask("Новое название", 3, "2024-12-31")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mockRepository.updateTaskDetails(
                taskId = "task789",
                userId = "test_user",
                title = "Новое название",
                priority = 3,
                dueDate = "2024-12-31",
                folderId = null
            )
        }
    }

    @Test
    fun `tasks are sorted by isDone and priority`() = runTest {
        val mockTasks = listOf(
            Task("1", "test_user", "Задача 1", true, 3, null, null, ""),
            Task("2", "test_user", "Задача 2", false, 1, null, null, ""),
            Task("3", "test_user", "Задача 3", false, 3, null, null, ""),
            Task("4", "test_user", "Задача 4", true, 1, null, null, "")
        )
        coEvery { mockRepository.getTasks("test_user") } returns Result.success(mockTasks)

        viewModel.loadTasks()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Задача 3", state.tasks[0].title)
        assertEquals("Задача 2", state.tasks[1].title)
        assertEquals("Задача 1", state.tasks[2].title)
        assertEquals("Задача 4", state.tasks[3].title)
    }

    @Test
    fun `updateSearchQuery updates search query state`() = runTest {
        viewModel.updateSearchQuery("Купить")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Купить", viewModel.searchQuery.value)
    }
}