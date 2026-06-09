package com.example.todolist.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private lateinit var context: Context
    private val databaseName = "migration-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrate1To2KeepsExistingTasksAndRemovesLegacyColumn() {
        val legacyDb = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        legacyDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tasks_cache (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                title TEXT NOT NULL,
                isDone INTEGER NOT NULL,
                priority INTEGER NOT NULL,
                dueDate TEXT,
                folderId TEXT,
                createdAt TEXT NOT NULL
            )
            """.trimIndent()
        )
        legacyDb.execSQL(
            """
            INSERT INTO tasks_cache (id, userId, title, isDone, priority, dueDate, folderId, createdAt)
            VALUES ('task-1', 'user-1', 'Finish report', 0, 2, '2026-06-10T12:00:00', 'folder-1', '2026-06-09T09:00:00')
            """.trimIndent()
        )
        legacyDb.execSQL("PRAGMA user_version = 1")
        legacyDb.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

        val migratedTasks = runBlocking {
            migratedDb.taskDao().getTasksByUserSync("user-1")
        }
        assertEquals(1, migratedTasks.size)
        assertEquals("task-1", migratedTasks.first().id)
        assertEquals("Finish report", migratedTasks.first().title)
        assertEquals("2026-06-10T12:00:00", migratedTasks.first().dueDate)
        assertEquals("NONE", migratedTasks.first().category)

        val columnsCursor = migratedDb.openHelper.writableDatabase.query("PRAGMA table_info(tasks_cache)")
        val columns = mutableListOf<String>()
        while (columnsCursor.moveToNext()) {
            columns += columnsCursor.getString(columnsCursor.getColumnIndexOrThrow("name"))
        }
        columnsCursor.close()

        assertFalse(columns.contains("folderId"))

        migratedDb.close()
    }

    @Test
    fun migrate2To3AddsCategoryColumnWithDefaultValue() {
        val version2Db = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        version2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tasks_cache (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                title TEXT NOT NULL,
                isDone INTEGER NOT NULL,
                priority INTEGER NOT NULL,
                dueDate TEXT,
                createdAt TEXT NOT NULL
            )
            """.trimIndent()
        )
        version2Db.execSQL(
            """
            INSERT INTO tasks_cache (id, userId, title, isDone, priority, dueDate, createdAt)
            VALUES ('task-2', 'user-2', 'Prepare slides', 0, 1, NULL, '2026-06-09T12:00:00')
            """.trimIndent()
        )
        version2Db.execSQL("PRAGMA user_version = 2")
        version2Db.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()

        val migratedTasks = runBlocking {
            migratedDb.taskDao().getTasksByUserSync("user-2")
        }
        assertEquals(1, migratedTasks.size)
        assertEquals("NONE", migratedTasks.first().category)

        migratedDb.close()
    }
}
