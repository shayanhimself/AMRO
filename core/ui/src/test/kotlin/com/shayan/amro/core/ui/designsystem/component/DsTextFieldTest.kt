package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.testing.assertion.assertTouchTargetMeetsMinimum
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val FIELD_LABEL = "Search"
private const val PLACEHOLDER = "Title or year"
private const val SUPPORTING_TEXT = "Two characters at least"
private const val TYPED = "Mongoose"
private const val CLEAR_LABEL = "Clear the field"

@RunWith(AndroidJUnit4::class)
class DsTextFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `typing reports what was typed`() {
        var reported = ""
        composeRule.setContent {
            AmroTheme {
                DsTextField(value = "", onValueChange = { reported = it }, label = FIELD_LABEL)
            }
        }

        composeRule.onNodeWithText(FIELD_LABEL).performTextInput(TYPED)

        assertEquals(TYPED, reported)
    }

    @Test
    fun `an unlabelled empty field shows its placeholder`() {
        composeRule.setContent {
            AmroTheme {
                DsTextField(value = "", onValueChange = {}, placeholder = PLACEHOLDER)
            }
        }

        composeRule.onNodeWithText(PLACEHOLDER).assertExists()
    }

    @Test
    fun `a field in error states what is wrong under it`() {
        composeRule.setContent {
            AmroTheme {
                DsTextField(
                    value = TYPED,
                    onValueChange = {},
                    label = FIELD_LABEL,
                    supportingText = SUPPORTING_TEXT,
                    isError = true,
                )
            }
        }

        composeRule.onNodeWithText(SUPPORTING_TEXT).assertExists()
    }

    @Test
    fun `the filled variant carries the same copy as the outlined one`() {
        composeRule.setContent {
            AmroTheme {
                DsTextField(
                    value = TYPED,
                    onValueChange = {},
                    label = FIELD_LABEL,
                    variant = TextFieldVariant.Filled,
                    supportingText = SUPPORTING_TEXT,
                )
            }
        }

        composeRule.onNodeWithText(TYPED).assertExists()
        composeRule.onNodeWithText(SUPPORTING_TEXT).assertExists()
    }

    @Test
    fun `an actionable trailing glyph reports its tap`() {
        var taps = 0
        composeRule.setContent {
            AmroTheme {
                DsTextField(
                    value = TYPED,
                    onValueChange = {},
                    trailingGlyph = Glyphs.CLOSE,
                    onTrailingClick = { taps++ },
                    trailingContentDescription = CLEAR_LABEL,
                )
            }
        }

        composeRule.onNodeWithContentDescription(CLEAR_LABEL).performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `an actionable trailing glyph is at least the minimum tappable size`() {
        composeRule.setContent {
            AmroTheme {
                DsTextField(
                    value = TYPED,
                    onValueChange = {},
                    trailingGlyph = Glyphs.CLOSE,
                    onTrailingClick = {},
                    trailingContentDescription = CLEAR_LABEL,
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(CLEAR_LABEL)
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    @Test
    fun `a decorative leading glyph is absent from the accessibility tree`() {
        composeRule.setContent {
            AmroTheme {
                DsTextField(
                    value = TYPED,
                    onValueChange = {},
                    leadingGlyph = Glyphs.MOVIE,
                )
            }
        }

        composeRule.onNodeWithText(Glyphs.MOVIE).assertDoesNotExist()
    }

    @Test
    fun `a disabled field takes no input`() {
        composeRule.setContent {
            AmroTheme {
                DsTextField(
                    value = TYPED,
                    onValueChange = {},
                    label = FIELD_LABEL,
                    enabled = false,
                )
            }
        }

        composeRule.onNodeWithText(TYPED).assertIsNotEnabled()
    }
}
