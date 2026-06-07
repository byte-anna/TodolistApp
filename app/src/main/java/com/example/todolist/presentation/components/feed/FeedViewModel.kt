package com.example.todolist.presentation.components.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.data.api.TodoApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.todolist.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val api: TodoApi
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
                handleError(e, isLoading = false)
            }
        }
    }

    private fun handleError(error: Throwable, isLoading: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            error = error.message,
            isLoading = isLoading,
            sessionExpired = error is SessionExpiredException
        )
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                api.toggleLike(postId)
                // Перезагружаем посты, чтобы обновить счетчик
                loadPosts()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}