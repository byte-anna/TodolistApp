package com.example.todolist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks_cache_new (
                        id TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        isDone INTEGER NOT NULL,
                        priority INTEGER NOT NULL,
                        dueDate TEXT,
                        createdAt TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO tasks_cache_new (id, userId, title, isDone, priority, dueDate, createdAt)
                    SELECT id, userId, title, isDone, priority, dueDate, createdAt
                    FROM tasks_cache
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE tasks_cache")
                db.execSQL("ALTER TABLE tasks_cache_new RENAME TO tasks_cache")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todolist_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
