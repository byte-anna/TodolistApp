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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SessionExpiredException : Exception("Сессия истекла, войдите снова")

class TodoApi(
    private val baseUrl: String = "http://10.0.2.2:8080",
    private val userPreferences: UserPreferences? = null
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

        install(io.ktor.client.plugins.HttpRequestRetry) {
            // Опционально: retry при ошибках
        }
    }

    private suspend fun getToken(): String? {
        return userPreferences?.authToken?.first()
    }

    private fun ensureAuthorized(response: HttpResponse) {
        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            throw SessionExpiredException()
        }
    }

    // === TASKS ===

    suspend fun getTasks(): List<Task> {
        val response = client.get("$baseUrl/tasks") {
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        ensureAuthorized(response)

        return response.body()
    }

    suspend fun createTask(
        title: String,
        priority: Int,
        dueDate: String? = null
    ): Task {
        Log.d("API_DEBUG", "➕ Отправляем на сервер:")
        Log.d("API_DEBUG", "   title=$title")
        Log.d("API_DEBUG", "   priority=$priority")
        Log.d("API_DEBUG", "   dueDate=$dueDate")

        val response = client.post("$baseUrl/tasks") {
            contentType(ContentType.Application.Json)
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(
                buildJsonObject {
                    put("title", title)
                    put("priority", priority)
                    dueDate?.let { put("dueDate", it) }
                }
            )
        }

        ensureAuthorized(response)

        val task = response.body<Task>()

        Log.d("API_DEBUG", "✅ Сервер вернул:")
        Log.d("API_DEBUG", "   id=${task.id}")
        Log.d("API_DEBUG", "   title=${task.title}")
        Log.d("API_DEBUG", "   dueDate=${task.dueDate}")

        return task
    }

    suspend fun updateTask(
        taskId: String,
        title: String? = null,
        isDone: Boolean? = null,
        priority: Int? = null,
        dueDate: String? = null
    ): Boolean {
        val request = buildJsonObject {
            title?.let { put("title", it) }
            isDone?.let { put("isDone", it) }
            priority?.let { put("priority", it) }
            dueDate?.let { put("dueDate", it) }
        }

        val response = client.put("$baseUrl/tasks/$taskId") {
            contentType(ContentType.Application.Json)
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(request)
        }

        ensureAuthorized(response)

        return response.status == HttpStatusCode.OK
    }

    suspend fun deleteTask(taskId: String): Boolean {
        val response = client.delete("$baseUrl/tasks/$taskId") {
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        ensureAuthorized(response)

        return response.status == HttpStatusCode.OK
    }

    // === AUTH ===

    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null
    ): AuthResponse {
        return client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password, displayName))
        }.body()
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthResponse {
        return client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
    }

    // === POSTS ===

    suspend fun createPost(
        content: String,
        taskId: String? = null
    ) {
        val response = client.post("$baseUrl/posts") {
            contentType(ContentType.Application.Json)
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(
                buildJsonObject {
                    put("content", content)
                    taskId?.let { put("taskId", it) }
                }
            )
        }

        ensureAuthorized(response)

        if (response.status != HttpStatusCode.Created && response.status != HttpStatusCode.OK) {
            val errorText = response.bodyAsText()
            throw IllegalStateException("Ошибка создания поста: ${response.status}. $errorText")
        }
    }

    suspend fun getPosts(): List<Post> {
        val response = client.get("$baseUrl/posts") {
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        ensureAuthorized(response)

        return response.body()
    }

    suspend fun toggleLike(postId: String) {
        try {
            val response = client.post("$baseUrl/posts/$postId/like") {
                contentType(ContentType.Application.Json)
                getToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            ensureAuthorized(response)
        } catch (e: SessionExpiredException) {
            throw e
        } catch (e: Exception) {
            // Пока оставляем без обработки, чтобы не делать большой рефакторинг.
        }
    }
}

// === REQUEST/RESPONSE MODELS ===

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val token: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)