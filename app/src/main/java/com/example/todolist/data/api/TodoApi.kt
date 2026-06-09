package com.example.todolist.data.api

import android.util.Log
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.model.TaskPriority
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime

class SessionExpiredException : Exception("Сессия истекла, войдите снова")

class TodoApi(
    private val baseUrl: String = "http://10.0.2.2:8080",
    private val userPreferences: UserPreferences? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("HTTP_CLIENT", message)
                }
            }
            level = LogLevel.INFO
        }
        install(io.ktor.client.plugins.HttpRequestRetry) {
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

    private suspend fun throwIfRequestFailed(response: HttpResponse, fallbackMessage: String) {
        ensureAuthorized(response)
        if (response.status.value !in 200..299) {
            val errorText = response.bodyAsText()
            val serverMessage = runCatching {
                json.decodeFromString<ErrorResponse>(errorText).error
            }.getOrNull()
            val message = serverMessage?.takeIf { it.isNotBlank() }
                ?: errorText.takeIf { it.isNotBlank() }
                ?: fallbackMessage
            throw IllegalStateException("$fallbackMessage: $message")
        }
    }

    private fun normalizeOutgoingPriority(priority: Int): Int {
        return TaskPriority.toApiValue(priority)
    }

    private fun requireIsoDueDate(dueDate: String?) {
        if (dueDate == null) return
        runCatching {
            LocalDateTime.parse(dueDate)
        }.getOrElse {
            throw IllegalArgumentException("Некорректный формат dueDate. Ожидается ISO LocalDateTime")
        }
    }

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
        dueDate: String? = null,
        category: TaskCategory = TaskCategory.NONE
    ): Task {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Название задачи не может быть пустым" }
        requireIsoDueDate(dueDate)

        val normalizedPriority = normalizeOutgoingPriority(priority)
        val requestBody = buildJsonObject {
            put("title", normalizedTitle)
            put("priority", normalizedPriority)
            put("category", category.name)
            dueDate?.let { put("dueDate", it) }
        }

        Log.d("API_DEBUG", "POST /tasks body=$requestBody")

        val response = client.post("$baseUrl/tasks") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(requestBody)
        }

        throwIfRequestFailed(response, "Ошибка создания задачи")
        return response.body()
    }

    suspend fun updateTask(
        taskId: String,
        title: String? = null,
        isDone: Boolean? = null,
        priority: Int? = null,
        dueDate: String? = null,
        category: TaskCategory? = null
    ): Boolean {
        title?.let {
            require(it.trim().isNotEmpty()) { "Название задачи не может быть пустым" }
        }
        requireIsoDueDate(dueDate)

        val request = buildJsonObject {
            title?.trim()?.let { put("title", it) }
            isDone?.let { put("isDone", it) }
            priority?.let { put("priority", normalizeOutgoingPriority(it)) }
            dueDate?.let { put("dueDate", it) }
            category?.let { put("category", it.name) }
        }

        Log.d("API_DEBUG", "PUT /tasks/$taskId body=$request")

        val response = client.put("$baseUrl/tasks/$taskId") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            getToken()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(request)
        }

        throwIfRequestFailed(response, "Ошибка обновления задачи")
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

    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null
    ): AuthResponse {
        return client.post("$baseUrl/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RegisterRequest(email, password, displayName))
        }.body()
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthResponse {
        return client.post("$baseUrl/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(LoginRequest(email, password))
        }.body()
    }

    suspend fun createPost(
        content: String,
        taskId: String? = null
    ) {
        val response = client.post("$baseUrl/posts") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
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

        throwIfRequestFailed(response, "Ошибка создания поста")
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
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                getToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            ensureAuthorized(response)
        } catch (e: SessionExpiredException) {
            throw e
        } catch (_: Exception) {
        }
    }
}

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
