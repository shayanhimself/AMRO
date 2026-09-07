package com.shayan.amro.core.ui.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.R
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TITLE = "The Mongoose"
private const val POSTER_URL = "https://image.example/w185/poster.jpg"

@RunWith(AndroidJUnit4::class)
class PosterTileTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a poster is described by the film it is for`() {
        setContent(posterUrl = POSTER_URL)

        composeRule
            .onNodeWithContentDescription(string(R.string.core_ui_poster_description, TITLE))
            .assertExists()
    }

    @Test
    fun `a film with no poster says so, and is not read as an image`() {
        setContent(posterUrl = null)

        composeRule.onNodeWithText(string(R.string.core_ui_no_poster)).assertExists()
        composeRule
            .onNodeWithContentDescription(string(R.string.core_ui_poster_description, TITLE))
            .assertDoesNotExist()
    }

    @Test
    fun `the placeholder glyph is absent from the accessibility tree`() {
        setContent(posterUrl = null)

        composeRule.onNodeWithText(Glyphs.MOVIE).assertDoesNotExist()
    }

    private fun setContent(posterUrl: String?) {
        composeRule.setContent {
            AmroTheme {
                PosterTile(posterUrl = posterUrl, title = TITLE)
            }
        }
    }
}
