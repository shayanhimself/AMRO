package com.shayan.amro.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.ui.designsystem.theme.AmroExtendedTheme
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** How large the block a test draws is, which only has to be a size the block cannot invent. */
private val BLOCK_SIZE = 8.dp

/** What a placeholder that is not breathing is drawn at. */
private const val STILL_ALPHA = 1f

private const val BLOCK_TAG = "placeholder-block"

@RunWith(AndroidJUnit4::class)
class PlaceholderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a block takes the size the caller lays it out at`() {
        composeRule.setContent {
            AmroTheme {
                PlaceholderBlock(
                    color = AmroExtendedTheme.colors.placeholderMuted,
                    modifier = Modifier.testTag(BLOCK_TAG).size(BLOCK_SIZE),
                )
            }
        }

        composeRule.onNodeWithTag(BLOCK_TAG).assertWidthIsEqualTo(BLOCK_SIZE)
        composeRule.onNodeWithTag(BLOCK_TAG).assertHeightIsEqualTo(BLOCK_SIZE)
    }

    @Test
    fun `a placeholder under inspection holds still`() {
        var alpha = 0f
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                alpha = placeholderPulseAlpha()
            }
        }

        assertEquals(STILL_ALPHA, alpha, 0f)
    }
}
