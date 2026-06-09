package com.example.todolist.presentation.components.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todolist.R
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.model.TaskPriority
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    userName: String? = null,
    onOpenFeed: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenStats: () -> Unit,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val viewModel: TasksViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val visibleTasks by viewModel.visibleTasks.collectAsState()
    val totalCount by viewModel.completedTasksCount.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    val displayName = remember(userName) {
        userName?.trim()?.takeIf { it.isNotEmpty() && !it.contains("@") }
    }

    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) {
            onSessionExpired()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        displayName?.let { name ->
                            Text(
                                text = stringResource(R.string.tasks_greeting, name),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(R.string.tasks_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenCalendar) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Календарный режим",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(
                            painter = painterResource(id = R.drawable.progress),
                            contentDescription = stringResource(R.string.stats_title),
                            tint = Color(0xFF7E57C2)
                        )
                    }
                    IconButton(onClick = onOpenFeed) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.tasks_open_feed),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.tasks_logout))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tasks_add))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tasks_search_label)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.tasks_search_icon)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.tasks_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Статус", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.updateFilter(filter) },
                        label = { Text(filter.toLabel()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Категория", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskCategoryFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedCategoryFilter == filter,
                        onClick = { viewModel.updateCategoryFilter(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box {
                AssistChip(
                    onClick = { showSortMenu = true },
                    label = { Text(stringResource(R.string.tasks_sort_label, selectedSort.toLabel())) }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    TaskSortOption.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.toLabel()) },
                            onClick = {
                                viewModel.updateSort(sort)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(Color.Red.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = error, color = Color.Red)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { viewModel.loadTasks() }) {
                                Text(stringResource(R.string.tasks_retry))
                            }
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(stringResource(R.string.tasks_close_error))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (visibleTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = emptyStateMessage(
                                searchQuery = searchQuery,
                                selectedCategoryFilter = selectedCategoryFilter
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        if (searchQuery.isEmpty()) {
                            TextButton(onClick = { viewModel.showAddDialog() }) {
                                Text(stringResource(R.string.tasks_create_first))
                            }
                        } else {
                            TextButton(onClick = { viewModel.clearSearch() }) {
                                Text(stringResource(R.string.tasks_clear_search))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Icon(
                                        painter = painterResource(id = R.drawable.check),
                                        contentDescription = stringResource(R.string.tasks_progress),
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.tasks_progress),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.tasks_completed_count, totalCount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { showClearDialog = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(
                                        stringResource(R.string.tasks_clear_all),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    items(visibleTasks, key = { it.id }) { task ->
                        val dismissState = rememberDismissState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart) {
                                    viewModel.deleteTask(task.id)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismiss(
                            state = dismissState,
                            background = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissState.targetValue) {
                                        DismissValue.Default -> Color.Transparent
                                        else -> Color.Red
                                    },
                                    label = "background color"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.tasks_delete),
                                        tint = Color.White
                                    )
                                }
                            },
                            dismissContent = {
                                TaskItem(
                                    task = task,
                                    onToggle = { viewModel.toggleTask(task.id, !task.isDone) },
                                    onEdit = { viewModel.showEditDialog(task) }
                                )
                            },
                            directions = setOf(DismissDirection.EndToStart)
                        )
                    }
                }
            }
        }

        uiState.dialogTask?.let { task ->
            val isEdit = task.id.isNotEmpty()
            AddTaskDialog(
                initialTitle = task.title,
                initialPriority = task.priority,
                initialDueDate = task.dueDate,
                initialCategory = task.category,
                isEdit = isEdit,
                onConfirm = { title, priority, dueDate, category, shareToFeed ->
                    if (isEdit) {
                        viewModel.updateTask(title, priority, dueDate, category)
                    } else {
                        viewModel.addTask(title, priority, dueDate, category, shareToFeed)
                    }
                },
                onDismiss = { viewModel.closeDialog() }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(stringResource(R.string.tasks_delete_all_title)) },
                text = { Text(stringResource(R.string.tasks_delete_all_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllTasks()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            stringResource(R.string.tasks_delete),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }
}

private fun emptyStateMessage(
    searchQuery: String,
    selectedCategoryFilter: TaskCategoryFilter
): String {
    return when {
        searchQuery.isNotEmpty() -> "Ничего не найдено по запросу \"$searchQuery\""
        selectedCategoryFilter != TaskCategoryFilter.ALL ->
            "В категории \"${selectedCategoryFilter.label}\" пока нет задач"
        else -> "Нет задач. Добавь первую!"
    }
}

private fun TaskFilter.toLabel(): String = when (this) {
    TaskFilter.ALL -> "Все"
    TaskFilter.ACTIVE -> "Активные"
    TaskFilter.COMPLETED -> "Выполненные"
    TaskFilter.OVERDUE -> "Просроченные"
}

private fun TaskSortOption.toLabel(): String = when (this) {
    TaskSortOption.PRIORITY -> "Приоритет"
    TaskSortOption.DEADLINE -> "Дедлайн"
    TaskSortOption.CREATED_AT -> "Дата создания"
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val priorityColor = when (TaskPriority.fromValue(task.priority)) {
        TaskPriority.HIGH -> Color.Red
        TaskPriority.MEDIUM -> Color(0xFFFFA500)
        TaskPriority.LOW -> Color.Green
    }

    val dueDateText = task.dueDate?.let { dateStr ->
        runCatching {
            val localDateTime = LocalDateTime.parse(dateStr)
            val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")
            "⏰ ${localDateTime.format(formatter)}"
        }.getOrElse {
            "⏰ $dateStr"
        }
    }

    val dueDateColor = if (task.dueDate != null) {
        runCatching {
            val deadline = LocalDateTime.parse(task.dueDate)
            val now = LocalDateTime.now()
            if (!task.isDone && deadline.isBefore(now)) Color.Red else Color.Gray
        }.getOrDefault(Color.Gray)
    } else {
        Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) {
                Color.LightGray.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("task_checkbox_${task.id}")
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (task.isDone) Color.Gray else Color.Black,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("task_edit_${task.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.tasks_edit),
                        tint = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(priorityColor, shape = CircleShape)
                )
            }
            dueDateText?.let { text ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = dueDateColor,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
            if (task.category != TaskCategory.NONE) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
        }
    }
}
