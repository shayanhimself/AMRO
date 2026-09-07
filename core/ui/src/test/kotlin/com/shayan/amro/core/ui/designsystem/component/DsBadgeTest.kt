package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val COUNT = "3"

@RunWith(AndroidJUnit4::class)
class DsBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every tone carries the label it is given`() {
        composeRule.setContent {
            AmroTheme {
                Column {
                    BadgeTone.entries.forEach { tone -> DsBadge(tone = tone, text = labelOf(tone)) }
                }
            }
        }

        BadgeTone.entries.forEach { tone ->
            composeRule.onNodeWithText(labelOf(tone)).assertExists()
        }
    }

    @Test
    fun `a badge with nothing to say is a bare dot`() {
        composeRule.setContent {
            AmroTheme {
                DsBadge()
            }
        }

        composeRule.onNodeWithText(COUNT).assertDoesNotExist()
    }
}

private fun labelOf(tone: BadgeTone) = "$COUNT ${tone.name}"
