package com.example.todolist.presentation.components.tasks

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TaskItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun taskItemCallsToggleWhenCheckboxClicked() {
        var toggleCalled = false

        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(id = "task-1", title = "Подготовить доклад"),
                    onToggle = { toggleCalled = true },
                    onEdit = {}
                )
            }
        }

        composeRule.onNodeWithTag("task_checkbox_task-1").performClick()

        composeRule.runOnIdle {
            assertTrue(toggleCalled)
        }
    }

    @Test
    fun taskItemCallsEditWhenEditButtonClicked() {
        var editCalls = 0

        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(id = "task-2", title = "Обновить презентацию"),
                    onToggle = {},
                    onEdit = { editCalls++ }
                )
            }
        }

        composeRule.onNodeWithTag("task_edit_task-2").performClick()

        composeRule.runOnIdle {
            assertEquals(1, editCalls)
        }
    }

    @Test
    fun taskItemShowsFormattedDeadline() {
        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(
                        id = "task-3",
                        title = "Сдать курсовую",
                        dueDate = "2026-06-10T14:30:00"
                    ),
                    onToggle = {},
                    onEdit = {}
                )
            }
        }

        composeRule.onNodeWithText("Сдать курсовую").assertIsDisplayed()
        composeRule.onNodeWithText("⏰ 10.06 14:30").assertIsDisplayed()
    }

    @Test
    fun taskItemShowsCategoryLabelWhenPresent() {
        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(
                        id = "task-4",
                        title = "Подготовить демо",
                        category = TaskCategory.STUDY
                    ),
                    onToggle = {},
                    onEdit = {}
                )
            }
        }

        composeRule.onNodeWithText("Учеба").assertIsDisplayed()
    }

    @Test
    fun taskItemHidesEditButtonWhenEditActionIsMissing() {
        composeRule.setContent {
            MaterialTheme {
                TaskItem(
                    task = task(id = "task-5", title = "Только просмотр"),
                    onToggle = {},
                    onEdit = null
                )
            }
        }

        composeRule.onAllNodesWithTag("task_edit_task-5").assertCountEquals(0)
    }

    private fun task(
        id: String,
        title: String,
        dueDate: String? = null,
        category: TaskCategory = TaskCategory.NONE
    ) = Task(
        id = id,
        userId = "user-1",
        title = title,
        isDone = false,
        priority = 1,
        dueDate = dueDate,
        createdAt = "2026-06-09T10:00:00",
        category = category
    )
}
