package com.shayan.amro.feature.discover.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.testing.string
import com.shayan.amro.core.ui.designsystem.theme.AmroTheme
import com.shayan.amro.core.ui.label.labelRes
import com.shayan.amro.feature.discover.R
import com.shayan.amro.feature.discover.viewmodel.DiscoverUiState
import com.shayan.amro.feature.discover.viewmodel.toDiscoverUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterSheetContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the sheet heads each choice it offers`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.feature_discover_sheet_title)).assertExists()
        composeRule
            .onNodeWithText(string(R.string.feature_discover_sheet_genre_heading))
            .assertExists()
        composeRule
            .onNodeWithText(string(R.string.feature_discover_sheet_sort_heading))
            .assertExists()
        composeRule
            .onNodeWithText(string(R.string.feature_discover_sheet_direction_heading))
            .assertExists()
    }

    @Test
    fun `choosing a genre reports the genre it stands for`() {
        var toggled: String? = null
        setContent(onToggleGenre = { toggled = it })

        composeRule.onNodeWithText(genreLabel(SELECTABLE_GENRE)).performScrollTo().performClick()

        assertEquals(SELECTABLE_GENRE.name, toggled)
    }

    @Test
    fun `choosing an order reports the key it stands for`() {
        var chosen: SortKey? = null
        setContent(onSelectSortKey = { chosen = it })

        composeRule
            .onNodeWithText(string(R.string.feature_discover_sort_title))
            .performScrollTo()
            .performClick()

        assertEquals(SortKey.TITLE, chosen)
    }

    @Test
    fun `choosing a direction reports the direction it stands for`() {
        var chosen: SortDirection? = null
        setContent(onSelectSortDirection = { chosen = it })

        composeRule
            .onNodeWithText(string(R.string.feature_discover_direction_ascending))
            .performScrollTo()
            .performClick()

        assertEquals(SortDirection.ASCENDING, chosen)
    }

    @Test
    fun `resetting reports that the sheet should be cleared`() {
        var resets = 0
        setContent(onReset = { resets++ })

        composeRule.onNodeWithText(string(R.string.feature_discover_sheet_reset)).performClick()

        assertEquals(1, resets)
    }

    /** Renders the sheet's content over one state. */
    private fun setContent(
        uiState: DiscoverUiState = filter(),
        onToggleGenre: (String) -> Unit = {},
        onSelectSortKey: (SortKey) -> Unit = {},
        onSelectSortDirection: (SortDirection) -> Unit = {},
        onReset: () -> Unit = {},
    ) {
        composeRule.setContent {
            AmroTheme {
                FilterSheetContent(
                    filter = uiState.filter,
                    onToggleGenre = onToggleGenre,
                    onSelectSortKey = onSelectSortKey,
                    onSelectSortDirection = onSelectSortDirection,
                    onReset = onReset,
                )
            }
        }
    }
}

/** A genre a test picks, where nothing about which one it is decides the test. */
private val SELECTABLE_GENRE = Genre.HORROR

/** The state the sheet reads its offers out of, built the way the screen builds it. */
private fun filter(genres: Set<Genre> = emptySet()) =
    MovieSelection(genres = genres).toDiscoverUiState()

private fun genreLabel(genre: Genre) = string(genre.labelRes)
