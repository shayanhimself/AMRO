package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.R
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LABEL = "Horror"

@RunWith(AndroidJUnit4::class)
class DsChipTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every variant reports the click of the chip it draws`() {
        val clicks = ChipVariant.entries.associateWith { 0 }.toMutableMap()
        composeRule.setContent {
            AmroTheme {
                Column {
                    ChipVariant.entries.forEach { variant ->
                        DsChip(
                            label = labelOf(variant),
                            onClick = { clicks[variant] = clicks.getValue(variant) + 1 },
                            variant = variant,
                        )
                    }
                }
            }
        }

        ChipVariant.entries.forEach { variant ->
            composeRule.onNodeWithText(labelOf(variant)).performClick()
        }

        assertEquals(ChipVariant.entries.associateWith { 1 }, clicks)
    }

    @Test
    fun `a selected filter chip says it is selected`() {
        composeRule.setContent {
            AmroTheme {
                DsChip(
                    label = LABEL,
                    onClick = {},
                    variant = ChipVariant.Filter,
                    selected = true,
                    leadingGlyph = Glyphs.CHECK,
                )
            }
        }

        composeRule.onNodeWithText(LABEL).assertIsSelected()
    }

    @Test
    fun `dismissing an input chip reports the dismissal`() {
        var dismissals = 0
        composeRule.setContent {
            AmroTheme {
                DsChip(
                    label = LABEL,
                    onClick = {},
                    variant = ChipVariant.Input,
                    onDismiss = { dismissals++ },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                string(R.string.core_ui_dismiss, LABEL),
            ).performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun `a disabled chip is not clickable`() {
        composeRule.setContent {
            AmroTheme {
                DsChip(label = LABEL, onClick = {}, enabled = false)
            }
        }

        composeRule.onNodeWithText(LABEL).assertIsNotEnabled()
    }
}

private fun labelOf(variant: ChipVariant) = "$LABEL ${variant.name}"
