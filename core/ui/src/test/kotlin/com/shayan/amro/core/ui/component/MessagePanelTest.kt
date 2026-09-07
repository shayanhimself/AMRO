package com.shayan.amro.core.ui.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.testing.assertion.assertTouchTargetMeetsMinimum
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.R
import com.shayan.amro.core.ui.designsystem.component.ButtonVariant
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TITLE = "No connection"
private const val BODY = "Filters look inside what is already here."

@RunWith(AndroidJUnit4::class)
class MessagePanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the one way out reports that it was taken`() {
        var actions = 0
        setContent(onAction = { actions++ })

        composeRule.onNodeWithText(actionLabel()).performClick()

        assertEquals(1, actions)
    }

    @Test
    fun `a title that says everything is shown without a body`() {
        setContent()

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(BODY).assertDoesNotExist()
    }

    @Test
    fun `a body is shown under the title where one is given`() {
        setContent(body = BODY)

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(BODY).assertExists()
    }

    @Test
    fun `the glyph is absent from the accessibility tree, as the title states it`() {
        setContent()

        composeRule.onNodeWithText(Glyphs.CLOUD_OFF).assertDoesNotExist()
    }

    @Test
    fun `the action is at least the minimum tappable size`() {
        setContent(actionVariant = ButtonVariant.Tonal)

        composeRule
            .onNodeWithText(actionLabel())
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    private fun setContent(
        body: String? = null,
        actionVariant: ButtonVariant = ButtonVariant.Filled,
        onAction: () -> Unit = {},
    ) {
        composeRule.setContent {
            AmroTheme {
                MessagePanel(
                    glyph = Glyphs.CLOUD_OFF,
                    title = TITLE,
                    actionLabel = actionLabel(),
                    onAction = onAction,
                    body = body,
                    actionVariant = actionVariant,
                )
            }
        }
    }
}

private fun actionLabel() = string(R.string.core_ui_retry)
