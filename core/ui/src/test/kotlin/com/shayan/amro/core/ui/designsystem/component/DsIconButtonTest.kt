package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LABEL = "Close the sheet"

@RunWith(AndroidJUnit4::class)
class DsIconButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every variant reports the click of the button it draws`() {
        val clicks = IconButtonVariant.entries.associateWith { 0 }.toMutableMap()
        composeRule.setContent {
            AmroTheme {
                Column {
                    IconButtonVariant.entries.forEach { variant ->
                        DsIconButton(
                            glyph = Glyphs.CLOSE,
                            contentDescription = labelOf(variant),
                            onClick = { clicks[variant] = clicks.getValue(variant) + 1 },
                            variant = variant,
                        )
                    }
                }
            }
        }

        IconButtonVariant.entries.forEach { variant ->
            composeRule.onNodeWithContentDescription(labelOf(variant)).performClick()
        }

        assertEquals(IconButtonVariant.entries.associateWith { 1 }, clicks)
    }

    @Test
    fun `a selected button keeps the description it carries unselected`() {
        composeRule.setContent {
            AmroTheme {
                DsIconButton(
                    glyph = Glyphs.STAR,
                    contentDescription = LABEL,
                    onClick = {},
                    selected = true,
                )
            }
        }

        composeRule.onNodeWithContentDescription(LABEL).assertExists()
    }

    @Test
    fun `a disabled button is not clickable`() {
        composeRule.setContent {
            AmroTheme {
                DsIconButton(
                    glyph = Glyphs.CLOSE,
                    contentDescription = LABEL,
                    onClick = {},
                    enabled = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription(LABEL).assertIsNotEnabled()
    }
}

private fun labelOf(variant: IconButtonVariant) = "$LABEL ${variant.name}"
