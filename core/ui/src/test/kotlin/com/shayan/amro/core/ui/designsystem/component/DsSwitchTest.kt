package com.shayan.amro.core.ui.designsystem.component

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DsSwitchTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `toggling an off switch asks for it to go on`() {
        var reported: Boolean? = null
        setContent(checked = false, onCheckedChange = { reported = it })

        composeRule.onNode(isToggleable()).assertIsOff().performClick()

        assertEquals(true, reported)
    }

    @Test
    fun `toggling an on switch asks for it to go off`() {
        var reported: Boolean? = null
        setContent(checked = true, onCheckedChange = { reported = it })

        composeRule.onNode(isToggleable()).assertIsOn().performClick()

        assertEquals(false, reported)
    }

    @Test
    fun `a disabled switch does not toggle`() {
        var reports = 0
        setContent(checked = false, enabled = false, onCheckedChange = { reports++ })

        composeRule.onNode(isToggleable()).assertIsNotEnabled().performClick()

        assertEquals(0, reports)
    }

    private fun setContent(
        checked: Boolean,
        enabled: Boolean = true,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        composeRule.setContent {
            AmroTheme {
                DsSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
            }
        }
    }
}
