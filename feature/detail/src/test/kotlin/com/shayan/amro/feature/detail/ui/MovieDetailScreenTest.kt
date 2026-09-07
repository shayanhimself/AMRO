package com.shayan.amro.feature.detail.ui

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.MovieDetail
import com.shayan.amro.core.testing.assertion.assertTouchTargetMeetsMinimum
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.detail.R
import com.shayan.amro.feature.detail.viewmodel.MovieDetailContent
import com.shayan.amro.feature.detail.viewmodel.MovieDetailUiState
import com.shayan.amro.feature.detail.viewmodel.toMovieDetailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import com.shayan.amro.core.ui.R as CoreUiR

@RunWith(AndroidJUnit4::class)
class MovieDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var density: Density = Density(density = 1f)

    @Test
    fun `the bar has no title at rest, and takes it once the header is scrolled past`() {
        setContent(loaded())

        composeRule.onAllNodesWithText(TITLE).assertCountEquals(1)

        composeRule.onNodeWithContentDescription(imdbLabel()).performScrollTo()

        composeRule.onAllNodesWithText(TITLE).assertCountEquals(2)
    }

    @Test
    fun `a first load names the movie in the header, and the poster it was opened from`() {
        setContent(firstLoad())

        // The header carries the title, so the bar stays at rest and does not state it twice.
        composeRule.onAllNodesWithText(TITLE).assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(posterLabel()).assertCountEquals(1)
        composeRule
            .onNodeWithContentDescription(string(CoreUiR.string.core_ui_loading))
            .assertExists()
    }

    @Test
    fun `the IMDB action reports the title's address`() {
        var opened: String? = null
        setContent(loaded(), onOpenImdb = { url -> opened = url })

        composeRule.onNodeWithContentDescription(imdbLabel()).performScrollTo().performClick()

        assertEquals(loadedContent().imdbUrl, opened)
    }

    @Test
    fun `back fires where there is a back action`() {
        var backs = 0
        setContent(loaded(), onBack = { backs++ })

        composeRule.onNodeWithContentDescription(backLabel()).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `a pane with nowhere to go back to grows no back action`() {
        setContent(loaded(), onBack = null)

        composeRule.onAllNodesWithContentDescription(backLabel()).assertCountEquals(0)
    }

    @Test
    fun `retry fires from the error state`() {
        var retries = 0
        setContent(failed(DataError.NoConnectivity), onRetry = { retries++ })

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `every action is tappable, the poster is described and the backdrop is not`() {
        setContent(loaded())

        composeRule.onNodeWithContentDescription(backLabel()).assertTouchTargetMeetsMinimum(density)
        composeRule
            .onNodeWithContentDescription(imdbLabel())
            .performScrollTo()
            .assertTouchTargetMeetsMinimum(density)

        // The poster names the film, and the backdrop beside it says nothing, so the film is
        // announced once rather than twice.
        composeRule.onAllNodesWithContentDescription(posterLabel()).assertCountEquals(1)
    }

    /**
     * Renders one state, and records the density its nodes were measured at.
     *
     * @param uiState what the screen renders.
     */
    private fun setContent(
        uiState: MovieDetailUiState,
        onRetry: () -> Unit = {},
        onOpenImdb: (url: String) -> Unit = {},
        onBack: (() -> Unit)? = {},
    ) {
        composeRule.setContent {
            density = LocalDensity.current
            AmroTheme {
                MovieDetailScreen(
                    uiState = uiState,
                    onRetry = onRetry,
                    onOpenImdb = onOpenImdb,
                    onBack = onBack,
                )
            }
        }
    }
}

/** The record every loaded state renders. */
private val DETAIL: MovieDetail = MovieFixture.detail()

/** The film the screen names, in the header and, once it is scrolled away, in the bar. */
private val TITLE = DETAIL.movie.title

/** The state a screen with the record cached renders. */
private fun loaded(): MovieDetailUiState =
    toMovieDetailUiState(movie = DETAIL.movie, detail = DETAIL, error = null)

/** The body of [loaded], which the address the IMDB action reports is read back from. */
private fun loadedContent(): MovieDetailContent.Loaded =
    loaded().content as MovieDetailContent.Loaded

/** The state a screen opened on a row whose record has not arrived renders. */
private fun firstLoad(): MovieDetailUiState =
    toMovieDetailUiState(movie = DETAIL.movie, detail = null, error = null)

/** The state a screen with nothing cached and a failed fetch renders. */
private fun failed(cause: DataError): MovieDetailUiState =
    toMovieDetailUiState(movie = DETAIL.movie, detail = null, error = cause)

/** What the back action is called. */
private fun backLabel(): String = string(R.string.feature_detail_back)

/** What the IMDB action is called, which is where it also says it leaves the app. */
private fun imdbLabel(): String = string(R.string.feature_detail_imdb_action_description)

/** What the poster is described as. */
private fun posterLabel(): String = string(CoreUiR.string.core_ui_poster_description, TITLE)
