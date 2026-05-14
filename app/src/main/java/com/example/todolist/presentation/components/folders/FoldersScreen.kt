package com.example.todolist.presentation.components.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todolist.domain.model.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onAddFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#6200EE") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Папки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Добавить папку")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет папок. Создай первую! 📁")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Все задачи (без папки)
                    item {
                        FolderItem(
                            name = "Все задачи",
                            color = "#9E9E9E",
                            onClick = { onFolderClick(Folder("", "", "Все задачи", "#9E9E9E", "")) },
                            showDelete = false
                        )
                    }

                    items(folders) { folder ->
                        FolderItem(
                            name = folder.name,
                            color = folder.color,
                            onClick = { onFolderClick(folder) },
                            onDelete = { onDeleteFolder(folder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новая папка") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Название папки") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Цвет:")
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorButton("#6200EE", selectedColor) { selectedColor = "#6200EE" }
                        ColorButton("#03DAC5", selectedColor) { selectedColor = "#03DAC5" }
                        ColorButton("#FF5722", selectedColor) { selectedColor = "#FF5722" }
                        ColorButton("#4CAF50", selectedColor) { selectedColor = "#4CAF50" }
                        ColorButton("#2196F3", selectedColor) { selectedColor = "#2196F3" }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onAddFolder(newFolderName.trim(), selectedColor)
                            newFolderName = ""
                            showAddDialog = false
                        }
                    },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun FolderItem(
    name: String,
    color: String,
    onClick: () -> Unit,
    showDelete: Boolean = true,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        try { Color(android.graphics.Color.parseColor(color)) }
                        catch (e: Exception) { Color.Gray },
                        CircleShape
                    )
            )
            Spacer(Modifier.width(12.dp))
            Text(text = name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))

            if (showDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Удалить", tint = Color.Red.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun ColorButton(colorHex: String, selected: String, onClick: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
    Card(
        modifier = Modifier.size(32.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected == colorHex) color else color.copy(alpha = 0.3f)),
        onClick = onClick
    ) {
        if (selected == colorHex) {
            Icon(Icons.Default.Check, "Выбрано", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}