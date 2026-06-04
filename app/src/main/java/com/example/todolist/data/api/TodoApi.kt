package com.example.todolist.data.api

import android.util.Log
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.model.Task
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TodoApi(
    private val baseUrl: String = "http://10.0.2.2:8080",
    private val userPreferences: UserPreferences? = null  // ← ДОБАВЛЕНО
) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        // ← ДОБАВЛЕНО: Interceptor для подстановки токена
        install(io.ktor.client.plugins.HttpRequestRetry) {
            // Опционально: retry при ошибках
        }
    }

    // ← ДОБАВЛЕНО: Метод для получения токена
    private suspend fun getToken(): String? {
        return userPreferences?.authToken?.first()
    }

    // === TASKS ===
    suspend fun getTasks(userId: String): List<Task> {
        return client.get("$baseUrl/tasks") {
            url { parameters.append("userId", userId) }
            // ← ДОБАВЛЕНО: Заголовок Authorization
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }.body()
    }

    suspend fun createTask(userId: String, title: String, priority: Int, dueDate: String? = null): Task {
        Log.d("API_DEBUG", "➕ Отправляем на сервер:")
        Log.d("API_DEBUG", "   title=$title")
        Log.d("API_DEBUG", "   priority=$priority")
        Log.d("API_DEBUG", "   dueDate=$dueDate")

        val response = client.post("$baseUrl/tasks") {
            url { parameters.append("userId", userId) }
            contentType(ContentType.Application.Json)
            // ← ДОБАВЛЕНО: Заголовок Authorization
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(CreateTaskRequest(title, priority, dueDate))
        }

        val task = response.body<Task>()
        Log.d("API_DEBUG", "✅ Сервер вернул:")
        Log.d("API_DEBUG", "   id=${task.id}")
        Log.d("API_DEBUG", "   title=${task.title}")
        Log.d("API_DEBUG", "   dueDate=${task.dueDate}")

        return task
    }

    suspend fun updateTask(
        taskId: String,
        userId: String,
        title: String? = null,
        isDone: Boolean? = null,
        priority: Int? = null,
        dueDate: String? = null
    ): Boolean {
        val request = UpdateTaskRequest(title, isDone, priority, dueDate)

        val response = client.put("$baseUrl/tasks/$taskId") {
            url { parameters.append("userId", userId) }
            contentType(ContentType.Application.Json)
            // ← ДОБАВЛЕНО: Заголовок Authorization
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(request)
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun deleteTask(taskId: String, userId: String): Boolean {
        val response = client.delete("$baseUrl/tasks/$taskId") {
            url { parameters.append("userId", userId) }
            // ← ДОБАВЛЕНО: Заголовок Authorization
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        return response.status == HttpStatusCode.OK
    }

    // === AUTH ===
    suspend fun register(email: String, password: String, displayName: String? = null): AuthResponse {
        return client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password, displayName))
        }.body()
    }

    suspend fun login(email: String, password: String): AuthResponse {
        return client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
    }

    suspend fun createPost(userId: String, content: String, taskId: String? = null) {
        try {
            client.post("$baseUrl/posts") {
                contentType(ContentType.Application.Json)
                // ← ДОБАВЛЕНО: Заголовок Authorization
                getToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(CreatePostRequest(userId, content, taskId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getPosts(): List<Post> {
        return client.get("$baseUrl/posts") {
            // ← ДОБАВЛЕНО: Заголовок Authorization
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }.body()
    }

    suspend fun toggleLike(postId: String, userId: String) {
        try {
            client.post("$baseUrl/posts/$postId/like") {
                contentType(ContentType.Application.Json)
                // ← ДОБАВЛЕНО: Заголовок Authorization
                getToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(mapOf("userId" to userId))
            }
        } catch (e: Exception) {
        }
    }
}

// === REQUEST/RESPONSE MODELS ===

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String? = null)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val token: String? = null  // ← ДОБАВЛЕНО!
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val priority: Int = 1,
    val dueDate: String? = null
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val isDone: Boolean? = null,
    val priority: Int? = null,
    val dueDate: String? = null
)

@Serializable
data class CreatePostRequest(
    val userId: String,
    val content: String,
    val taskId: String? = null
)

@Serializable
data class ErrorResponse(val error: String)