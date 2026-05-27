package com.example.todolist.presentation.components.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Post
import java.time.format.DateTimeFormatter
import com.example.todolist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    api: TodoApi,
    userId: String,
    onBackClick: () -> Unit
) {
    val viewModel: FeedViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FeedViewModel(api, userId) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Лента достижений",
                            tint = MaterialTheme.colorScheme.primary  // ← Или onSurface
                        )
                        Text("Лента достижений")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize()) {

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(Color.Red.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = uiState.error!!, color = Color.Red)
                        TextButton(onClick = { viewModel.loadPosts() }) {
                            Text("Повторить")
                        }
                    }
                }
            } else if (uiState.posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Пока нет достижений. Будь первым!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Текст достижения
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Дата и лайки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPostDate(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

// Форматирование даты
@Composable
fun formatPostDate(dateString: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val localDateTime = java.time.LocalDateTime.parse(dateString)
        "🕐 ${localDateTime.format(formatter)}"
    } catch (e: Exception) {
        "🕐 $dateString"
    }
}