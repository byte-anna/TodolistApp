package com.example.todolist.presentation.components.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Folder
import com.example.todolist.domain.model.Task
import com.example.todolist.utils.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dialogTask: Task? = null
)

class TasksViewModel(
    private val api: TodoApi,
    private val userId: String,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init { loadTasks() }

    fun loadTasks() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val tasks = api.getTasks(userId)
                val filteredTasks = if (currentFolderId == null) {
                    tasks
                } else {
                    tasks.filter { it.folderId == currentFolderId }
                }
                _uiState.value = _uiState.value.copy(
                    tasks = filteredTasks.sortedWith(
                        compareBy<Task> { it.isDone }.thenByDescending { it.priority }
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun addTask(title: String, priority: Int, dueDate: String? = null, folderId: String? = null, shareToFeed: Boolean = false) {
        viewModelScope.launch {
            try {
                val createdTask = api.createTask(userId, title, priority, dueDate, folderId)
                loadTasks()
                closeDialog()
                if (shareToFeed) {
                    api.createPost(userId, createdTask.title, createdTask.id)
                }
                if (dueDate != null) {
                    NotificationScheduler.scheduleReminder(
                        getApplication<Application>(),
                        createdTask.id,
                        title,
                        dueDate
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateTask(title: String, priority: Int, dueDate: String? = null, folderId: String? = null) {
        val task = _uiState.value.dialogTask ?: return
        viewModelScope.launch {
            try {
                api.updateTask(
                    taskId = task.id,
                    userId = userId,
                    title = title,
                    priority = priority,
                    dueDate = dueDate,
                    folderId = folderId
                )
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleTask(taskId: String, isDone: Boolean) {
        viewModelScope.launch {
            try {
                api.updateTask(
                    taskId = taskId,
                    userId = userId,
                    isDone = isDone,
                    folderId = null
                )
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                api.deleteTask(taskId, userId)
                NotificationScheduler.cancelReminder(getApplication<Application>(), taskId)
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    var currentFolderId: String? = null
        private set

    fun loadFolders() {
        viewModelScope.launch {
            try {
                val foldersList = api.getFolders(userId)
                _folders.value = foldersList
            } catch (e: Exception) {
                // Игнорируем ошибку
            }
        }
    }

    fun setCurrentFolder(folderId: String?) {
        currentFolderId = folderId
        loadTasks()
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
            folderId = null,
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }


    val completedTasksCount: StateFlow<Int> = uiState.map { state ->
        state.tasks.count { it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun clearAllTasks() {
        viewModelScope.launch {
            try {
                // Получаем список всех текущих задач
                val tasks = _uiState.value.tasks

                // Удаляем каждую задачу на сервере и отменяем уведомления
                for (task in tasks) {
                    api.deleteTask(task.id, userId)
                    NotificationScheduler.cancelReminder(getApplication<Application>(), task.id)
                }

                // Перезагружаем список
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }


}