package com.example.todolist.presentation.components.tasks

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddTaskDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addTaskDialogSubmitsSelectedPriorityTitleAndCategory() {
        var submittedTitle: String? = null
        var submittedPriority: Int? = null
        var submittedCategory: TaskCategory? = null

        composeRule.setContent {
            MaterialTheme {
                AddTaskDialog(
                    initialTitle = "",
                    initialPriority = TaskPriority.MEDIUM.value,
                    isEdit = false,
                    onConfirm = { title, priority, _, category, _ ->
                        submittedTitle = title
                        submittedPriority = priority
                        submittedCategory = category
                    },
                    onDismiss = {}
                )
            }
        }

        composeRule.onAllNodesWithText("Добавить")[0].assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextInput("Подготовить защиту")
        composeRule.onNodeWithText("Работа").performClick()
        composeRule.onNodeWithText("Высокий").performClick()
        composeRule.onAllNodesWithText("Добавить")[0].performClick()

        composeRule.runOnIdle {
            assertEquals("Подготовить защиту", submittedTitle)
            assertEquals(TaskPriority.HIGH.value, submittedPriority)
            assertEquals(TaskCategory.WORK, submittedCategory)
        }
    }

    @Test
    fun editModeShowsSaveButton() {
        composeRule.setContent {
            MaterialTheme {
                AddTaskDialog(
                    initialTitle = "Обновить слайды",
                    initialPriority = TaskPriority.MEDIUM.value,
                    isEdit = true,
                    onConfirm = { _, _, _, _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Сохранить").assertIsDisplayed()
    }

    @Test
    fun addTaskDialogPassesShareToFeedFlagWhenChecked() {
        var shareToFeed: Boolean? = null

        composeRule.setContent {
            MaterialTheme {
                AddTaskDialog(
                    initialTitle = "",
                    initialPriority = TaskPriority.MEDIUM.value,
                    isEdit = false,
                    onConfirm = { _, _, _, _, share ->
                        shareToFeed = share
                    },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("task_title_input").performTextInput("Опубликовать достижение")
        composeRule.onNodeWithTag("share_to_feed_checkbox").performClick()
        composeRule.onAllNodesWithText("Добавить")[0].performClick()

        composeRule.runOnIdle {
            assertEquals(true, shareToFeed)
        }
    }
}
