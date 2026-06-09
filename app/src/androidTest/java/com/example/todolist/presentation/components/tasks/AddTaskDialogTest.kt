package com.example.todolist.presentation.components.tasks

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.todolist.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddTaskDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addTaskDialog_submitsSelectedPriorityAndTitle() {
        var submittedTitle: String? = null
        var submittedPriority: Int? = null

        composeRule.setContent {
            MaterialTheme {
                AddTaskDialog(
                    initialTitle = "",
                    initialPriority = TaskPriority.MEDIUM.value,
                    isEdit = false,
                    onConfirm = { title, priority, _, _ ->
                        submittedTitle = title
                        submittedPriority = priority
                    },
                    onDismiss = {}
                )
            }
        }

        composeRule.onAllNodesWithText("Добавить")[0].assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextInput("Подготовить защиту")
        composeRule.onNodeWithText("Высокий").performClick()
        composeRule.onAllNodesWithText("Добавить")[0].performClick()

        composeRule.runOnIdle {
            assertEquals("Подготовить защиту", submittedTitle)
            assertEquals(TaskPriority.HIGH.value, submittedPriority)
        }
    }

    @Test
    fun editMode_showsSaveButton() {
        composeRule.setContent {
            MaterialTheme {
                AddTaskDialog(
                    initialTitle = "Обновить слайды",
                    initialPriority = TaskPriority.MEDIUM.value,
                    isEdit = true,
                    onConfirm = { _, _, _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Сохранить").assertIsDisplayed()
    }
}
