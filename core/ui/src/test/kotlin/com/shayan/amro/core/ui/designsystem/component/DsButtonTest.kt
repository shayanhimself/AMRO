package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsNotEnabled
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

private const val LABEL = "Retry"

@RunWith(AndroidJUnit4::class)
class DsButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every variant reports the click of the button it draws`() {
        val clicks = ButtonVariant.entries.associateWith { 0 }.toMutableMap()
        composeRule.setContent {
            AmroTheme {
                Column {
                    ButtonVariant.entries.forEach { variant ->
                        DsButton(
                            text = labelOf(variant),
                            onClick = { clicks[variant] = clicks.getValue(variant) + 1 },
                            variant = variant,
                        )
                    }
                }
            }
        }

        ButtonVariant.entries.forEach { variant ->
            composeRule.onNodeWithText(labelOf(variant)).performClick()
        }

        assertEquals(ButtonVariant.entries.associateWith { 1 }, clicks)
    }

    @Test
    fun `a loading button ignores the click`() {
        var clicks = 0
        composeRule.setContent {
            AmroTheme {
                DsButton(text = LABEL, onClick = { clicks++ }, loading = true)
            }
        }

        composeRule.onNodeWithText(LABEL).performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun `a disabled button is not clickable`() {
        composeRule.setContent {
            AmroTheme {
                DsButton(text = LABEL, onClick = {}, enabled = false)
            }
        }

        composeRule.onNodeWithText(LABEL).assertIsNotEnabled()
    }

    @Test
    fun `the glyphs either side of the label stay out of the accessibility tree`() {
        composeRule.setContent {
            AmroTheme {
                DsButton(
                    text = LABEL,
                    onClick = {},
                    leadingGlyph = Glyphs.REFRESH,
                    trailingGlyph = Glyphs.ARROW_FORWARD,
                )
            }
        }

        composeRule.onNodeWithText(LABEL).assertExists()
        composeRule.onNodeWithText(Glyphs.REFRESH).assertDoesNotExist()
        composeRule.onNodeWithText(Glyphs.ARROW_FORWARD).assertDoesNotExist()
    }
}

private fun labelOf(variant: ButtonVariant) = "$LABEL ${variant.name}"
