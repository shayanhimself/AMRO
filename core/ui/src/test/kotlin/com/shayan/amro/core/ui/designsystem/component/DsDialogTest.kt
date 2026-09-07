package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TITLE = "Clear your filters?"
private const val BODY = "The list goes back to everything trending."
private const val CONFIRM = "Clear"
private const val DISMISS = "Keep them"

@RunWith(AndroidJUnit4::class)
class DsDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `confirming reports the confirmation`() {
        var confirmations = 0
        setContent(onConfirm = { confirmations++ })

        composeRule.onNodeWithText(CONFIRM).performClick()

        assertEquals(1, confirmations)
    }

    @Test
    fun `a dialog offering no second action shows only the confirmation`() {
        setContent()

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(CONFIRM).assertExists()
        composeRule.onNodeWithText(DISMISS).assertDoesNotExist()
    }

    @Test
    fun `the body and the glyph are drawn where they are given`() {
        setContent(text = BODY, glyph = Glyphs.FILTER_ALT_OFF)

        composeRule.onNodeWithText(BODY).assertExists()
        // The title states the icon, so the glyph carries no description of its own.
        composeRule.onNodeWithText(Glyphs.FILTER_ALT_OFF).assertDoesNotExist()
    }

    @Test
    fun `the second action reports its own dismissal`() {
        var dismissals = 0
        var requests = 0
        setContent(
            dismissText = DISMISS,
            onDismiss = { dismissals++ },
            onDismissRequest = { requests++ },
        )

        composeRule.onNodeWithText(DISMISS).performClick()

        assertEquals(1, dismissals)
        assertEquals(0, requests)
    }

    @Test
    fun `a second action with no handler of its own falls back to the dismiss request`() {
        var requests = 0
        setContent(dismissText = DISMISS, onDismissRequest = { requests++ })

        composeRule.onNodeWithText(DISMISS).performClick()

        assertEquals(1, requests)
    }

    private fun setContent(
        text: String? = null,
        glyph: String? = null,
        dismissText: String? = null,
        onConfirm: () -> Unit = {},
        onDismiss: (() -> Unit)? = null,
        onDismissRequest: () -> Unit = {},
    ) {
        composeRule.setContent {
            AmroTheme {
                DsDialog(
                    onDismissRequest = onDismissRequest,
                    title = TITLE,
                    confirmText = CONFIRM,
                    onConfirm = onConfirm,
                    text = text,
                    glyph = glyph,
                    dismissText = dismissText,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}
