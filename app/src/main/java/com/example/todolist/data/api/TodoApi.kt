package com.example.todolist.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.Folder
import com.example.todolist.domain.model.Post

class TodoApi(
    private val baseUrl: String = "http://10.0.2.2:8080"
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
    }

    // === TASKS ===
    suspend fun getTasks(userId: String): List<Task> {
        return client.get("$baseUrl/tasks") {
            url { parameters.append("userId", userId) }
        }.body()
    }

    suspend fun createTask(userId: String, title: String, priority: Int, dueDate: String? = null, folderId: String? = null): Task {
        return client.post("$baseUrl/tasks") {
            url { parameters.append("userId", userId) }
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title, priority, dueDate, folderId))
        }.body()
    }

    suspend fun updateTask(
        taskId: String,
        userId: String,
        title: String? = null,
        isDone: Boolean? = null,
        priority: Int? = null,
        dueDate: String? = null,
        folderId: String? = null
    ): Boolean {
        val request = UpdateTaskRequest(title, isDone, priority, dueDate, folderId)

        val response = client.put("$baseUrl/tasks/$taskId") {
            url { parameters.append("userId", userId) }
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status == HttpStatusCode.OK
    }

    suspend fun deleteTask(taskId: String, userId: String): Boolean {
        val response = client.delete("$baseUrl/tasks/$taskId") {
            url { parameters.append("userId", userId) }
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

    // === FOLDERS ===
    suspend fun getFolders(userId: String): List<Folder> {
        return client.get("$baseUrl/folders") {
            url { parameters.append("userId", userId) }
        }.body()
    }

    suspend fun createFolder(userId: String, name: String, color: String): Folder {
        return client.post("$baseUrl/folders") {
            url { parameters.append("userId", userId) }
            contentType(ContentType.Application.Json)
            setBody(CreateFolderRequest(name, color))
        }.body()
    }

    suspend fun deleteFolder(folderId: String, userId: String): Boolean {
        val response = client.delete("$baseUrl/folders/$folderId") {
            url { parameters.append("userId", userId) }
        }
        return response.status == HttpStatusCode.OK
    }
    suspend fun createPost(userId: String, content: String, taskId: String? = null) {
        try {

            val response = client.post("$baseUrl/posts") {
                contentType(ContentType.Application.Json)
                setBody(CreatePostRequest(userId, content, taskId))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getPosts(): List<Post> {
        return client.get("$baseUrl/posts").body()
    }

    suspend fun toggleLike(postId: String, userId: String) {
        try {
            client.post("$baseUrl/posts/$postId/like") {
                contentType(ContentType.Application.Json)
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
data class AuthResponse(val userId: String, val email: String, val displayName: String? = null)


@Serializable
data class CreateTaskRequest(
    val title: String,
    val priority: Int = 1,
    val dueDate: String? = null,
    val folderId: String? = null
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val isDone: Boolean? = null,
    val priority: Int? = null,
    val dueDate: String? = null,
    val folderId: String? = null
)

@Serializable
data class CreateFolderRequest(val name: String, val color: String = "#6200EE")
@Serializable
data class CreatePostRequest(
    val userId: String,
    val content: String,
    val taskId: String? = null
)


@Serializable
data class ErrorResponse(val error: String)