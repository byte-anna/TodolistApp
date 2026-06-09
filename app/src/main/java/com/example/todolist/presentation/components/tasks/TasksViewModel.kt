package com.example.todolist.presentation.components.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.model.TaskPriority
import com.example.todolist.domain.usecase.reminder.CancelReminderUseCase
import com.example.todolist.domain.usecase.reminder.ScheduleReminderUseCase
import com.example.todolist.domain.usecase.session.ObserveUserIdUseCase
import com.example.todolist.domain.usecase.tasks.CreatePostUseCase
import com.example.todolist.domain.usecase.tasks.CreateTaskUseCase
import com.example.todolist.domain.usecase.tasks.DeleteTaskUseCase
import com.example.todolist.domain.usecase.tasks.GetTasksUseCase
import com.example.todolist.domain.usecase.tasks.UpdateTaskDetailsUseCase
import com.example.todolist.domain.usecase.tasks.UpdateTaskStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dialogTask: Task? = null,
    val sessionExpired: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val updateTaskDetailsUseCase: UpdateTaskDetailsUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val observeUserIdUseCase: ObserveUserIdUseCase,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
    private val cancelReminderUseCase: CancelReminderUseCase
) : ViewModel() {

    val userId: StateFlow<String> = observeUserIdUseCase()
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(TaskSortOption.PRIORITY)
    val selectedSort: StateFlow<TaskSortOption> = _selectedSort.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow(TaskCategoryFilter.ALL)
    val selectedCategoryFilter: StateFlow<TaskCategoryFilter> = _selectedCategoryFilter.asStateFlow()

    val visibleTasks: StateFlow<List<Task>> = combine(
        uiState,
        searchQuery,
        selectedFilter,
        selectedSort,
        selectedCategoryFilter
    ) { state, query, filter, sort, categoryFilter ->
        state.tasks
            .filterByQuery(query)
            .filterByType(filter)
            .filterByCategory(categoryFilter)
            .sortByOption(sort)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val completedTasksCount: StateFlow<Int> = uiState.map { state ->
        state.tasks.count { it.isDone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadTasks()
    }

    fun updateTask(
        title: String,
        priority: Int,
        dueDate: String? = null,
        category: TaskCategory = TaskCategory.NONE
    ) {
        val task = _uiState.value.dialogTask ?: return
        viewModelScope.launch {
            updateTaskDetailsUseCase(
                taskId = task.id,
                title = title,
                priority = priority,
                dueDate = dueDate,
                category = category
            ).onSuccess {
                loadTasks()
                closeDialog()
                cancelReminderUseCase(task.id)
                if (dueDate != null) {
                    scheduleReminderUseCase(task.id, title, dueDate)
                }
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun loadTasks() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            getTasksUseCase()
                .onSuccess { tasks ->
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    handleError(error, isLoading = false)
                }
        }
    }

    fun addTask(
        title: String,
        priority: Int,
        dueDate: String? = null,
        category: TaskCategory = TaskCategory.NONE,
        shareToFeed: Boolean = false
    ) {
        viewModelScope.launch {
            createTaskUseCase(title, priority, dueDate, category)
                .onSuccess { createdTask ->
                    loadTasks()
                    closeDialog()

                    if (shareToFeed) {
                        createPostUseCase(createdTask.title, createdTask.id)
                            .onFailure { error ->
                                handleError(error)
                                return@launch
                            }
                    }

                    if (dueDate != null) {
                        scheduleReminderUseCase(createdTask.id, title, dueDate)
                    }
                }
                .onFailure { error ->
                    handleError(error)
                }
        }
    }

    fun toggleTask(taskId: String, isDone: Boolean) {
        viewModelScope.launch {
            updateTaskStatusUseCase(taskId, isDone)
                .onSuccess {
                    loadTasks()
                }
                .onFailure { error ->
                    handleError(error)
                }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
                .onSuccess {
                    cancelReminderUseCase(taskId)
                    loadTasks()
                }
                .onFailure { error ->
                    handleError(error)
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun updateFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun updateSort(sort: TaskSortOption) {
        _selectedSort.value = sort
    }

    fun updateCategoryFilter(filter: TaskCategoryFilter) {
        _selectedCategoryFilter.value = filter
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            dialogTask = Task(
                id = "",
                userId = "",
                title = "",
                isDone = false,
                priority = TaskPriority.MEDIUM.value,
                dueDate = null,
                createdAt = "",
                category = TaskCategory.NONE
            )
        )
    }

    fun showEditDialog(task: Task) {
        _uiState.value = _uiState.value.copy(dialogTask = task)
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(dialogTask = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, sessionExpired = false)
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            val tasks = _uiState.value.tasks
            for (task in tasks) {
                deleteTaskUseCase(task.id)
                    .onFailure { error ->
                        handleError(error)
                        return@launch
                    }
                cancelReminderUseCase(task.id)
            }
            loadTasks()
        }
    }

    private fun handleError(error: Throwable, isLoading: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            error = error.message,
            isLoading = isLoading,
            sessionExpired = error is SessionExpiredException
        )
    }

    private fun List<Task>.filterByQuery(query: String): List<Task> {
        if (query.isBlank()) return this
        return filter { task ->
            task.title.contains(query, ignoreCase = true) ||
                task.category.label.contains(query, ignoreCase = true)
        }
    }

    private fun List<Task>.filterByType(filter: TaskFilter): List<Task> {
        return when (filter) {
            TaskFilter.ALL -> this
            TaskFilter.ACTIVE -> filter { !it.isDone }
            TaskFilter.COMPLETED -> filter { it.isDone }
            TaskFilter.OVERDUE -> filter { task ->
                !task.isDone && task.dueDate?.let(::parseDateTime)?.isBefore(LocalDateTime.now()) == true
            }
        }
    }

    private fun List<Task>.filterByCategory(filter: TaskCategoryFilter): List<Task> {
        val targetCategory = filter.category ?: return this
        return filter { it.category == targetCategory }
    }

    private fun List<Task>.sortByOption(sort: TaskSortOption): List<Task> {
        return when (sort) {
            TaskSortOption.PRIORITY -> sortedWith(
                compareBy<Task> { it.isDone }
                    .thenByDescending { it.priority }
                    .thenByDescending { parseDateTime(it.createdAt) }
            )
            TaskSortOption.DEADLINE -> sortedWith(
                compareBy<Task> { it.isDone }
                    .thenBy { parseDateTime(it.dueDate) ?: LocalDateTime.MAX }
                    .thenByDescending { it.priority }
            )
            TaskSortOption.CREATED_AT -> sortedWith(
                compareBy<Task> { it.isDone }
                    .thenByDescending { parseDateTime(it.createdAt) }
            )
        }
    }

    private fun parseDateTime(value: String?): LocalDateTime? {
        return runCatching {
            value?.let(LocalDateTime::parse)
        }.getOrNull()
    }
}
