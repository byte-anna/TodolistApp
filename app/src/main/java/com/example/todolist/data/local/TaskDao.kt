package com.example.todolist.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks_cache WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTasksByUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks_cache WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getTasksByUserSync(userId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks_cache WHERE userId = :userId")
    suspend fun deleteTasksByUser(userId: String)

    @Query("DELETE FROM tasks_cache WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("UPDATE tasks_cache SET isDone = :isDone WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, isDone: Boolean)

    @Query("UPDATE tasks_cache SET title = :title, priority = :priority, dueDate = :dueDate WHERE id = :taskId")
    suspend fun updateTaskDetails(
        taskId: String,
        title: String,
        priority: Int,
        dueDate: String?
    )
}