package com.example.todolist.presentation.components.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Task
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import android.app.Application

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    userId: String,
    api: TodoApi,
    onLogout: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext as Application

    val viewModel: TasksViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TasksViewModel(api, userId, appContext) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    // ✅ Получаем searchQuery из ViewModel как StateFlow
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои задачи") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Выйти")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, "Добавить задачу")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize()) {

            // ✅ 1. ПОИСКОВАЯ СТРОКА
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    viewModel.updateSearchQuery(newValue)  // ✅ Вызываем метод ViewModel
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск задачи...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {  // ✅ Вызываем метод
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
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

            // ✅ 2. ОБРАБОТКА ОШИБОК
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
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Закрыть")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ✅ 3. ЛОГИКА СПИСКА + ФИЛЬТРАЦИЯ
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // ✅ Фильтруем задачи (ОБЪЯВЛЯЕМ ТОЛЬКО ОДИН РАЗ!)
                val filteredTasks = uiState.tasks.filter { task ->
                    task.title.contains(searchQuery, ignoreCase = true)
                }

                if (filteredTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Нет задач. Добавь первую! 👆"
                                else "Ничего не найдено по запросу \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredTasks, key = { it.id }) { task ->
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
                                        modifier = Modifier.fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Удалить",
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
        }

        // ✅ ДИАЛОГ
        if (uiState.dialogTask != null) {
            val task = uiState.dialogTask!!
            val isEdit = task.id.isNotEmpty()

            AddTaskDialog(
                initialTitle = task.title,
                initialPriority = task.priority,
                initialDueDate = task.dueDate,
                isEdit = isEdit,
                onConfirm = { title, priority, dueDate ->
                    if (isEdit) {
                        viewModel.updateTask(title, priority, dueDate, task.folderId)
                    } else {
                        viewModel.addTask(title, priority, dueDate, null)
                    }
                },
                onDismiss = { viewModel.closeDialog() }
            )
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val priorityColor = when (task.priority) {
        1 -> Color.Red
        2 -> Color(0xFFFFA500)
        3 -> Color.Green
        else -> Color.Gray
    }

    val dueDateText = task.dueDate?.let { dateStr ->
        try {
            val localDateTime = java.time.LocalDateTime.parse(dateStr)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm")
            "⏰ ${localDateTime.format(formatter)}"
        } catch (e: Exception) {
            "⏰ $dateStr"
        }
    }

    val dueDateColor = if (task.dueDate != null) {
        try {
            val deadline = java.time.LocalDateTime.parse(task.dueDate)
            val now = java.time.LocalDateTime.now()
            if (!task.isDone && deadline.isBefore(now)) Color.Red else Color.Gray
        } catch (e: Exception) { Color.Gray }
    } else { Color.Gray }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone)
                Color.LightGray.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (task.isDone) Color.Gray else Color.Black,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color.Gray)
                }
                Box(
                    modifier = Modifier.size(12.dp).background(priorityColor, shape = CircleShape)
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
        }
    }
}