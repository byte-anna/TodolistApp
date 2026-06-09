package com.example.todolist.presentation.components.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.api.SessionExpiredException
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.usecase.feed.GetPostsUseCase
import com.example.todolist.domain.usecase.feed.TogglePostLikeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            getPostsUseCase()
                .onSuccess { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    handleError(error, isLoading = false)
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
            togglePostLikeUseCase(postId)
                .onSuccess {
                    loadPosts()
                }
                .onFailure { error ->
                    handleError(error)
                }
        }
    }
}
