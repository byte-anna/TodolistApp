package com.example.todolist.presentation.components.feed

import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.FeedRepository
import com.example.todolist.domain.usecase.feed.GetPostsUseCase
import com.example.todolist.domain.usecase.feed.TogglePostLikeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private lateinit var viewModel: FeedViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { feedRepository.getPosts() } returns Result.success(emptyList())

        viewModel = FeedViewModel(
            getPostsUseCase = GetPostsUseCase(feedRepository),
            togglePostLikeUseCase = TogglePostLikeUseCase(feedRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPosts updates state with posts`() = runTest {
        val posts = listOf(
            Post(
                id = "post-1",
                userId = "user-1",
                content = "Completed sprint report",
                createdAt = "2026-06-09T10:00:00",
                likesCount = 3
            )
        )
        coEvery { feedRepository.getPosts() } returns Result.success(posts)

        viewModel.loadPosts()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.posts.size)
        assertEquals("Completed sprint report", state.posts.first().content)
    }

    @Test
    fun `toggleLike calls repository and reloads feed`() = runTest {
        coEvery { feedRepository.toggleLike("post-1") } returns Result.success(Unit)
        coEvery { feedRepository.getPosts() } returns Result.success(emptyList())

        viewModel.toggleLike("post-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { feedRepository.toggleLike("post-1") }
        coVerify(atLeast = 1) { feedRepository.getPosts() }
    }

    @Test
    fun `loadPosts handles repository error`() = runTest {
        coEvery { feedRepository.getPosts() } returns Result.failure(Exception("Feed error"))

        viewModel.loadPosts()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.error != null)
    }
}
