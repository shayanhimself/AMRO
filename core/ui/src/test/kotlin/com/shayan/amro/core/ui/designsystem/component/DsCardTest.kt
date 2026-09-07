package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val CARD_BODY = "Trending this week"

@RunWith(AndroidJUnit4::class)
class DsCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every variant draws the body it is given`() {
        composeRule.setContent {
            AmroTheme {
                Column {
                    CardVariant.entries.forEach { variant ->
                        DsCard(variant = variant) { Text(bodyOf(variant)) }
                    }
                }
            }
        }

        CardVariant.entries.forEach { variant ->
            composeRule.onNodeWithText(bodyOf(variant)).assertExists()
        }
    }

    @Test
    fun `every variant reports the click of the card it draws`() {
        val clicks = CardVariant.entries.associateWith { 0 }.toMutableMap()
        composeRule.setContent {
            AmroTheme {
                Column {
                    CardVariant.entries.forEach { variant ->
                        DsCard(
                            variant = variant,
                            onClick = { clicks[variant] = clicks.getValue(variant) + 1 },
                        ) { Text(bodyOf(variant)) }
                    }
                }
            }
        }

        CardVariant.entries.forEach { variant ->
            composeRule.onNodeWithText(bodyOf(variant)).performClick()
        }

        assertEquals(CardVariant.entries.associateWith { 1 }, clicks)
    }

    @Test
    fun `a card with no handler is not clickable`() {
        composeRule.setContent {
            AmroTheme {
                DsCard { Text(CARD_BODY) }
            }
        }

        composeRule.onNodeWithText(CARD_BODY).assertHasNoClickAction()
    }

    @Test
    fun `a card with a handler is clickable`() {
        composeRule.setContent {
            AmroTheme {
                DsCard(onClick = {}) { Text(CARD_BODY) }
            }
        }

        composeRule.onNodeWithText(CARD_BODY).assertHasClickAction()
    }
}

private fun bodyOf(variant: CardVariant) = "$CARD_BODY ${variant.name}"
