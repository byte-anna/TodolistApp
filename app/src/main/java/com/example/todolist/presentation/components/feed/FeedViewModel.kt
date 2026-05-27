package com.example.todolist.presentation.components.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.TodoApi
import com.example.todolist.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FeedViewModel(
    private val api: TodoApi,
    private val userId: String  // ✅ Добавили userId
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val posts = api.getPosts()
                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                api.toggleLike(postId, userId)
                // Перезагружаем посты, чтобы обновить счетчик
                loadPosts()
            } catch (e: Exception) {
            }
        }
    }
}