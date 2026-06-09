package com.example.todolist.presentation.components.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.todolist.R
import com.example.todolist.domain.model.TaskPriority
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    initialTitle: String,
    initialPriority: Int,
    initialDueDate: String? = null,
    isEdit: Boolean,
    onConfirm: (String, Int, String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var priority by remember { mutableIntStateOf(initialPriority) }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var shareToFeed by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.let {
            try {
                LocalDateTime.parse(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    )

    val currentTime = LocalDateTime.now()
    val timePickerState = rememberTimePickerState(
        initialHour = dueDate?.let {
            try {
                LocalDateTime.parse(it).hour
            } catch (_: Exception) {
                currentTime.hour
            }
        } ?: currentTime.hour,
        initialMinute = dueDate?.let {
            try {
                LocalDateTime.parse(it).minute
            } catch (_: Exception) {
                currentTime.minute
            }
        } ?: currentTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Редактировать задачу" else "Новая задача") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDateTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
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
                                } catch (_: Exception) {
                                    it
                                }
                            } ?: "Выбрать дату и время",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dueDate == null) {
                                Color.Gray
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                Text("Приоритет:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityButton(
                        label = TaskPriority.HIGH.label,
                        color = Color.Red,
                        selected = priority == TaskPriority.HIGH.value
                    ) {
                        priority = TaskPriority.HIGH.value
                    }
                    PriorityButton(
                        label = TaskPriority.MEDIUM.label,
                        color = Color(0xFFFFA500),
                        selected = priority == TaskPriority.MEDIUM.value
                    ) {
                        priority = TaskPriority.MEDIUM.value
                    }
                    PriorityButton(
                        label = TaskPriority.LOW.label,
                        color = Color.Green,
                        selected = priority == TaskPriority.LOW.value
                    ) {
                        priority = TaskPriority.LOW.value
                    }
                }

                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

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
                        onConfirm(title.trim(), priority, dueDate, shareToFeed)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEdit) "Сохранить" else "Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

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
                TextButton(
                    onClick = {
                        val dateMillis = datePickerState.selectedDateMillis
                        if (dateMillis != null) {
                            val date = Instant.ofEpochMilli(dateMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val dateTime = LocalDateTime.of(
                                date.year,
                                date.month,
                                date.dayOfMonth,
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            dueDate = dateTime.toString()
                        }
                        showDateTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dueDate = null
                        showDateTimePicker = false
                    }
                ) {
                    Text("Очистить")
                }
            }
        )
    }
}

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
