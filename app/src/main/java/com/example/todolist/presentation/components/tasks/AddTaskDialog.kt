package com.example.todolist.presentation.components.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.todolist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    initialTitle: String,
    initialPriority: Int,
    initialDueDate: String? = null,
    isEdit: Boolean,
    onConfirm: (String, Int, String?, Boolean) -> Unit,  // ✅ 3 параметра
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var priority by remember { mutableIntStateOf(initialPriority) }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var shareToFeed by remember { mutableStateOf(false) }

    // DatePicker состояние
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.let {
            try {
                LocalDateTime.parse(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) { null }
        }
    )

    // TimePicker состояние
    val currentTime = LocalDateTime.now()
    val timePickerState = rememberTimePickerState(
        initialHour = dueDate?.let {
            try { LocalDateTime.parse(it).hour } catch (e: Exception) { currentTime.hour }
        } ?: currentTime.hour,
        initialMinute = dueDate?.let {
            try { LocalDateTime.parse(it).minute } catch (e: Exception) { currentTime.minute }
        } ?: currentTime.minute,
        is24Hour = true
    )

    var showDateTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать задачу" else "Новая задача") },
        text = {
            Column {
                // Название задачи
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                // Кнопка выбора даты и времени
                OutlinedButton(
                    onClick = { showDateTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        // ✅ Иконка + текст вместо эмодзи
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.calendar),
                                contentDescription = "Дедлайн",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Дедлайн:")
                        }

                        Text(
                            text = dueDate?.let {
                                try {
                                    val dt = LocalDateTime.parse(it)
                                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                    dt.format(formatter)
                                } catch (e: Exception) { it }
                            } ?: "Выбрать дату и время",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dueDate == null) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Приоритет
                Text("Приоритет:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    PriorityButton("Высокий", Color.Red, priority == 1) { priority = 1 }
                    PriorityButton("Средний", Color(0xFFFFA500), priority == 2) { priority = 2 }
                    PriorityButton("Низкий", Color.Green, priority == 3) { priority = 3 }
                }

                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

// ✅ Чекбокс "Поделиться"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = shareToFeed,
                        onCheckedChange = { shareToFeed = it }
                    )
                    Text(
                        text = "Опубликовать как достижение",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        // ✅ Передаём shareToFeed последним параметром
                        onConfirm(title.trim(), priority, dueDate, shareToFeed)
                    }
                },
                enabled = title.isNotBlank()
            ) { Text(if (isEdit) "Сохранить" else "Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    // Диалог выбора даты и времени
    if (showDateTimePicker) {
        AlertDialog(
            onDismissRequest = { showDateTimePicker = false },
            title = { Text("Выберите дату и время") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DatePicker(state = datePickerState)
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis
                    if (dateMillis != null) {
                        val date = Instant.ofEpochMilli(dateMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val dateTime = LocalDateTime.of(
                            date.year, date.month, date.dayOfMonth,
                            timePickerState.hour, timePickerState.minute
                        )
                        dueDate = dateTime.toString()
                    }
                    showDateTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dueDate = null
                    showDateTimePicker = false
                }) { Text("Очистить") }
            }
        )
    }
}

// ✅ Вспомогательная кнопка приоритета (только одна!)
@Composable
fun PriorityButton(
    label: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color else color.copy(alpha = 0.2f)
        ),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(12.dp),
            color = if (selected) Color.White else color
        )
    }
}