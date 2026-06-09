package com.example.todolist.presentation.components.tasks

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskPriority
import com.example.todolist.domain.repository.SessionRepository
import com.example.todolist.domain.repository.TaskRepository
import com.example.todolist.domain.service.ReminderScheduler
import com.example.todolist.domain.usecase.reminder.CancelReminderUseCase
import com.example.todolist.domain.usecase.reminder.ScheduleReminderUseCase
import com.example.todolist.domain.usecase.session.ObserveUserIdUseCase
import com.example.todolist.domain.usecase.tasks.CreatePostUseCase
import com.example.todolist.domain.usecase.tasks.CreateTaskUseCase
import com.example.todolist.domain.usecase.tasks.DeleteTaskUseCase
import com.example.todolist.domain.usecase.tasks.GetTasksUseCase
import com.example.todolist.domain.usecase.tasks.UpdateTaskDetailsUseCase
import com.example.todolist.domain.usecase.tasks.UpdateTaskStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository = mockk<TaskRepository>(relaxed = true)
    private val mockSessionRepository = mockk<SessionRepository>(relaxed = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { mockSessionRepository.userId } returns flowOf("test_user")
        every { reminderScheduler.cancel(any()) } returns Unit
        every { reminderScheduler.schedule(any(), any(), any()) } returns Unit
        coEvery { mockRepository.getTasks() } returns Result.success(emptyList())

        viewModel = TasksViewModel(
            getTasksUseCase = GetTasksUseCase(mockRepository),
            createTaskUseCase = CreateTaskUseCase(mockRepository),
            updateTaskStatusUseCase = UpdateTaskStatusUseCase(mockRepository),
            updateTaskDetailsUseCase = UpdateTaskDetailsUseCase(mockRepository),
            deleteTaskUseCase = DeleteTaskUseCase(mockRepository),
            createPostUseCase = CreatePostUseCase(mockRepository),
            observeUserIdUseCase = ObserveUserIdUseCase(mockSessionRepository),
            scheduleReminderUseCase = ScheduleReminderUseCase(reminderScheduler),
            cancelReminderUseCase = CancelReminderUseCase(reminderScheduler)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty tasks`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.tasks.isEmpty())
    }

    @Test
    fun `toggleTask calls updateTask repository with correct parameters`() = runTest {
        coEvery { mockRepository.updateTask(any(), any()) } returns Result.success(true)

        viewModel.toggleTask("task123", true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.updateTask("task123", true) }
    }

    @Test
    fun `addTask calls repository and reloads tasks`() = runTest {
        val mockTask = Task(
            id = "1",
            userId = "test_user",
            title = "Новая задача",
            isDone = false,
            priority = TaskPriority.HIGH.value,
            dueDate = null,
            createdAt = null
        )
        coEvery { mockRepository.createTask(any(), any(), any()) } returns Result.success(mockTask)

        viewModel.addTask("Новая задача", TaskPriority.HIGH.value, null, false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mockRepository.createTask("Новая задача", TaskPriority.HIGH.value, null)
        }
    }

    @Test
    fun `deleteTask calls deleteTask repository`() = runTest {
        coEvery { mockRepository.deleteTask(any()) } returns Result.success(true)

        viewModel.deleteTask("task456")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.deleteTask("task456") }
        every { reminderScheduler.cancel("task456") } returns Unit
    }

    @Test
    fun `loadTasks updates state with fetched tasks`() = runTest {
        val mockTasks = listOf(
            Task("1", "test_user", "Задача 1", false, TaskPriority.MEDIUM.value, null, null),
            Task("2", "test_user", "Задача 2", true, TaskPriority.HIGH.value, null, null)
        )
        coEvery { mockRepository.getTasks() } returns Result.success(mockTasks)

        viewModel.loadTasks()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.tasks.size)
        assertEquals("Задача 1", state.tasks[0].title)
    }

    @Test
    fun `loadTasks handles repository error gracefully`() = runTest {
        coEvery { mockRepository.getTasks() } returns Result.failure(Exception("Network error"))

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
        assertEquals(TaskPriority.MEDIUM.value, state.dialogTask!!.priority)
    }

    @Test
    fun `updateTask calls updateTaskDetails with correct parameters`() = runTest {
        val task = Task(
            id = "task789",
            userId = "test_user",
            title = "Старая задача",
            isDone = false,
            priority = TaskPriority.MEDIUM.value,
            dueDate = null,
            createdAt = null
        )
        viewModel.showEditDialog(task)

        coEvery {
            mockRepository.updateTaskDetails(any(), any(), any(), any())
        } returns Result.success(true)

        viewModel.updateTask("Новое название", TaskPriority.HIGH.value, "2024-12-31")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mockRepository.updateTaskDetails(
                taskId = "task789",
                title = "Новое название",
                priority = TaskPriority.HIGH.value,
                dueDate = "2024-12-31"
            )
        }
    }

    @Test
    fun `tasks are sorted by isDone and priority`() = runTest {
        val mockTasks = listOf(
            Task("1", "test_user", "Задача 1", true, TaskPriority.HIGH.value, null, null),
            Task("2", "test_user", "Задача 2", false, TaskPriority.LOW.value, null, null),
            Task("3", "test_user", "Задача 3", false, TaskPriority.HIGH.value, null, null),
            Task("4", "test_user", "Задача 4", true, TaskPriority.LOW.value, null, null)
        )
        coEvery { mockRepository.getTasks() } returns Result.success(mockTasks)

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
