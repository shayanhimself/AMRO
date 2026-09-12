package com.shayan.amro.feature.discover.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.discover.viewmodel.toRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import com.shayan.amro.core.ui.R as CoreUiR

/** The rows the list is fed. */
private val ROWS = MovieFixture.rankedMovies(count = 3).map { it.toRow() }

@RunWith(AndroidJUnit4::class)
class MovieListTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a row states its movie and reports it when it is chosen`() {
        var opened: String? = null
        setContent(onMovieClick = { opened = it })

        composeRule.onNodeWithText(ROWS.first().title).performClick()

        assertEquals(ROWS.first().movieId, opened)
    }

    @Test
    fun `every poster is described for a reader`() {
        setContent()

        ROWS.forEach { row ->
            composeRule
                .onNodeWithContentDescription(posterLabel(row.title), useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun `the row the pane beside the list is showing is the selected one`() {
        setContent(selectedMovieId = ROWS.first().movieId)

        composeRule.onNodeWithText(ROWS.first().title).assertIsSelected()
        composeRule.onNodeWithText(ROWS[1].title).assertIsNotSelected()
    }

    /** Renders the list over the rows every test here reads. */
    private fun setContent(
        selectedMovieId: String? = null,
        onMovieClick: (movieId: String) -> Unit = {},
    ) {
        composeRule.setContent {
            AmroTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MovieList(
                        rows = movieRows(ROWS),
                        selectedMovieId = selectedMovieId,
                        isRefreshing = false,
                        onRefresh = {},
                        onMovieClick = onMovieClick,
                        contentPadding = PaddingValues(),
                    )
                }
            }
        }
    }
}

private fun posterLabel(title: String) = string(CoreUiR.string.core_ui_poster_description, title)
