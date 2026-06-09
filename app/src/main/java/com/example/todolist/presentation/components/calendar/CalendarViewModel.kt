package com.example.todolist.presentation.components.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.usecase.tasks.GetTasksUseCase
import com.example.todolist.domain.usecase.tasks.UpdateTaskStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalendarUiState(
    val allTasks: List<Task> = emptyList(),
    val tasksForSelectedDate: List<Task> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            getTasksUseCase()
                .onSuccess { tasks ->
                    val selectedDate = _uiState.value.selectedDate
                    _uiState.value = _uiState.value.copy(
                        allTasks = tasks,
                        tasksForSelectedDate = tasks.filterByDate(selectedDate),
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                        sessionExpired = error is SessionExpiredException
                    )
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            tasksForSelectedDate = _uiState.value.allTasks.filterByDate(date)
        )
    }

    fun showPreviousDay() {
        selectDate(_uiState.value.selectedDate.minusDays(1))
    }

    fun showNextDay() {
        selectDate(_uiState.value.selectedDate.plusDays(1))
    }

    fun showToday() {
        selectDate(LocalDate.now())
    }

    fun toggleTask(taskId: String, isDone: Boolean) {
        viewModelScope.launch {
            updateTaskStatusUseCase(taskId, isDone)
                .onSuccess { loadTasks() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        sessionExpired = error is SessionExpiredException
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, sessionExpired = false)
    }

    private fun List<Task>.filterByDate(date: LocalDate): List<Task> {
        return filter { task ->
            runCatching {
                task.dueDate?.let(LocalDateTime::parse)?.toLocalDate() == date
            }.getOrDefault(false)
        }.sortedWith(
            compareBy<Task> { it.isDone }
                .thenByDescending { it.priority }
                .thenBy { task ->
                    runCatching {
                        task.dueDate?.let(LocalDateTime::parse)
                    }.getOrNull() ?: LocalDateTime.MAX
                }
        )
    }
}
