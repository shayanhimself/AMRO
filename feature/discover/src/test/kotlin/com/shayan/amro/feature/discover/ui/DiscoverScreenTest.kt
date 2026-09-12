package com.shayan.amro.feature.discover.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.data.DataErrorException
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.testing.assertion.assertTouchTargetMeetsMinimum
import com.shayan.amro.core.testing.quantityString
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.feature.discover.R
import com.shayan.amro.feature.discover.component.movieRows
import com.shayan.amro.feature.discover.listLoadStates
import com.shayan.amro.feature.discover.viewmodel.DiscoverBody
import com.shayan.amro.feature.discover.viewmodel.discoverBody
import com.shayan.amro.feature.discover.viewmodel.toDiscoverUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import com.shayan.amro.core.ui.R as CoreUiR

/** How many rows a list has when the case under test is about there being some. */
private const val ROW_COUNT = 3

/** A list with nothing further to serve, which is where a read of its head settles. */
private val SETTLED = LoadState.NotLoading(endOfPaginationReached = true)

@RunWith(AndroidJUnit4::class)
class DiscoverScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `nothing narrowing the list leaves the filter action unbadged`() {
        setContent(body = loaded())

        composeRule.onNodeWithContentDescription(filterLabel()).assertExists()
    }

    @Test
    fun `the badge counts the genres and the sort together`() {
        setContent(
            selection =
                MovieSelection(
                    genres = setOf(Genre.COMEDY, Genre.HORROR),
                    sort = MovieSort(SortKey.TITLE, SortDirection.ASCENDING),
                ),
            body = loaded(),
        )

        composeRule.onNodeWithContentDescription(filterLabel(active = 3)).assertExists()
        composeRule.onNodeWithText("3").assertExists()
    }

    @Test
    fun `retry fires from the notice bar`() {
        var refreshes = 0
        setContent(
            body = discoverBody(listLoadStates(failure(DataError.NoConnectivity)), ROW_COUNT),
            onRefresh = { refreshes++ },
        )

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertEquals(1, refreshes)
    }

    @Test
    fun `retry fires from the error state`() {
        var refreshes = 0
        setContent(
            body = discoverBody(listLoadStates(failure(DataError.Server)), itemCount = 0),
            onRefresh = { refreshes++ },
        )

        composeRule.onNodeWithText(string(R.string.feature_discover_error_server)).assertExists()
        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertEquals(1, refreshes)
    }

    @Test
    fun `a first load draws placeholder rows, and says so`() {
        setContent(body = discoverBody(listLoadStates(LoadState.Loading), itemCount = 0))

        composeRule
            .onNodeWithContentDescription(string(CoreUiR.string.core_ui_loading))
            .assertExists()
    }

    @Test
    fun `a selection the catalogue answered nothing for offers a way out of it`() {
        var cleared = 0
        setContent(
            selection = MovieSelection(genres = setOf(Genre.WESTERN)),
            body = discoverBody(listLoadStates(SETTLED), itemCount = 0),
            onClearGenres = { cleared++ },
        )

        composeRule
            .onNodeWithText(string(R.string.feature_discover_empty_filter_title))
            .assertExists()
        val clearFilters =
            composeRule.onNodeWithText(
                string(R.string.feature_discover_clear_filters),
            )
        clearFilters.assertTouchTargetMeetsMinimum(composeRule.density)
        clearFilters.performClick()

        assertEquals(1, cleared)
    }

    @Test
    fun `the notice bar is absent while a fetch runs over rows already on screen`() {
        setContent(body = discoverBody(listLoadStates(LoadState.Loading), itemCount = ROW_COUNT))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).assertDoesNotExist()
    }

    @Test
    fun `the filter action is at least the minimum tappable size`() {
        setContent(body = loaded())

        composeRule
            .onNodeWithContentDescription(filterLabel())
            .assertTouchTargetMeetsMinimum(composeRule.density)
    }

    /**
     * Renders the stateless screen over one selection and one body.
     *
     * What the screen does with the rows themselves is
     * [com.shayan.amro.feature.discover.component.MovieList]'s own test.
     */
    private fun setContent(
        body: DiscoverBody,
        selection: MovieSelection = MovieSelection(),
        onRefresh: () -> Unit = {},
        onClearGenres: () -> Unit = {},
    ) {
        composeRule.setContent {
            AmroTheme {
                DiscoverScreen(
                    uiState = selection.toDiscoverUiState(),
                    body = body,
                    rows = movieRows(emptyList()),
                    selectedMovieId = null,
                    onMovieClick = {},
                    onRefresh = onRefresh,
                    onToggleGenre = {},
                    onSelectSortKey = {},
                    onSelectSortDirection = {},
                    onClearGenres = onClearGenres,
                    onReset = {},
                )
            }
        }
    }
}

/** A settled list holding rows. */
private fun loaded() = discoverBody(listLoadStates(SETTLED), itemCount = ROW_COUNT)

/** A read of the head of the list that failed for [cause]. */
private fun failure(cause: DataError) = LoadState.Error(DataErrorException(cause))

/**
 * What the filter action is called.
 *
 * @param active how many genres are selected, plus one for a sort away from the default. Zero
 * leaves the action unbadged.
 */
private fun filterLabel(active: Int = 0) =
    if (active == 0) {
        string(R.string.feature_discover_filter_action)
    } else {
        quantityString(R.plurals.feature_discover_filter_action_active, active, active)
    }
