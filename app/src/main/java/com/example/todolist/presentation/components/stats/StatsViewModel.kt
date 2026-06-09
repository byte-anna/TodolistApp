package com.example.todolist.presentation.components.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskPriority
import com.example.todolist.domain.usecase.tasks.GetTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val totalTasks: Int = 0,
    val activeTasks: Int = 0,
    val completedTasks: Int = 0,
    val overdueTasks: Int = 0,
    val highPriorityTasks: Int = 0,
    val mediumPriorityTasks: Int = 0,
    val lowPriorityTasks: Int = 0,
    val completionPercent: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            getTasksUseCase()
                .onSuccess { tasks ->
                    _uiState.value = tasks.toStatsState()
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

    private fun List<Task>.toStatsState(): StatsUiState {
        val total = size
        val active = count { !it.isDone }
        val completed = count { it.isDone }
        val overdue = count { task ->
            !task.isDone && task.dueDate?.let(::parseDateTime)?.isBefore(LocalDateTime.now()) == true
        }
        val high = count { it.priority == TaskPriority.HIGH.value }
        val medium = count { it.priority == TaskPriority.MEDIUM.value }
        val low = count { it.priority == TaskPriority.LOW.value }
        val completionPercent = if (total == 0) 0 else (completed * 100) / total

        return StatsUiState(
            totalTasks = total,
            activeTasks = active,
            completedTasks = completed,
            overdueTasks = overdue,
            highPriorityTasks = high,
            mediumPriorityTasks = medium,
            lowPriorityTasks = low,
            completionPercent = completionPercent,
            isLoading = false
        )
    }

    private fun parseDateTime(value: String?): LocalDateTime? {
        return runCatching {
            value?.let(LocalDateTime::parse)
        }.getOrNull()
    }
}
