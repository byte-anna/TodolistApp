package com.example.todolist.presentation.components.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository
import com.example.todolist.utils.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dialogTask: Task? = null,
    val sessionExpired: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val userPreferences: UserPreferences,
    application: Application
) : AndroidViewModel(application) {

    val userId: StateFlow<String> = userPreferences.userId
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init { loadTasks() }

    fun updateTask(title: String, priority: Int, dueDate: String? = null) {
        val task = _uiState.value.dialogTask ?: return
        viewModelScope.launch {
            val result = repository.updateTaskDetails(
                taskId = task.id,
                title = title,
                priority = priority,
                dueDate = dueDate
            )
            result.onSuccess {
                loadTasks()
                closeDialog()
                NotificationScheduler.cancelReminder(getApplication<Application>(), task.id)
                if (dueDate != null) {
                    NotificationScheduler.scheduleReminder(
                        getApplication<Application>(),
                        task.id,
                        title,
                        dueDate
                    )
                }
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun loadTasks() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = repository.getTasks()
            result.onSuccess { tasks ->
                _uiState.value = _uiState.value.copy(
                    tasks = tasks.sortedWith(
                        compareBy<Task> { it.isDone }.thenByDescending { it.priority }
                    ),
                    isLoading = false
                )
            }.onFailure { error ->
                handleError(error, isLoading = false)
            }
        }
    }

    fun addTask(title: String, priority: Int, dueDate: String? = null, shareToFeed: Boolean = false) {
        viewModelScope.launch {
            val result = repository.createTask(title, priority, dueDate)
            result.onSuccess { createdTask ->
                loadTasks()
                closeDialog()

                // Создаём пост в ленте, если нужно
                if (shareToFeed) {
                    repository.createPost(createdTask.title, createdTask.id)
                        .onFailure { error ->
                            handleError(error)
                            return@launch
                        }
                }

                // Планируем напоминание
                if (dueDate != null) {
                    NotificationScheduler.scheduleReminder(
                        getApplication<Application>(),
                        createdTask.id,
                        title,
                        dueDate
                    )
                }
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun toggleTask(taskId: String, isDone: Boolean) {
        viewModelScope.launch {
            val result = repository.updateTask(taskId, isDone)
            result.onSuccess {
                loadTasks()
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = repository.deleteTask(taskId)
            result.onSuccess {
                NotificationScheduler.cancelReminder(getApplication<Application>(), taskId)
                loadTasks()
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun showAddDialog() {
        val dummyTask = Task(
            id = "",
            userId = "",
            title = "",
            isDone = false,
            priority = 2,
            dueDate = null,
            createdAt = ""
        )
        _uiState.value = _uiState.value.copy(dialogTask = dummyTask)
    }

    fun showEditDialog(task: Task) {
        _uiState.value = _uiState.value.copy(dialogTask = task)
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(dialogTask = null)
    }

    private fun handleError(error: Throwable, isLoading: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            error = error.message,
            isLoading = isLoading,
            sessionExpired = error is SessionExpiredException
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, sessionExpired = false)
    }

    val completedTasksCount: StateFlow<Int> = uiState.map { state ->
        state.tasks.count { it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun clearAllTasks() {
        viewModelScope.launch {
            val tasks = _uiState.value.tasks
            for (task in tasks) {
                val result = repository.deleteTask(task.id)
                result.onFailure { error ->
                    handleError(error)
                    return@launch
                }
                NotificationScheduler.cancelReminder(getApplication<Application>(), task.id)
            }
            loadTasks()
        }
    }
}