package com.shayan.amro.feature.discover.viewmodel

import androidx.paging.LoadState
import com.shayan.amro.core.data.DataErrorException
import com.shayan.amro.core.model.DataError
import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.MovieSelection
import com.shayan.amro.core.model.MovieSort
import com.shayan.amro.core.model.SortDirection
import com.shayan.amro.core.model.SortKey
import com.shayan.amro.core.testing.fixture.model.MovieFixture
import com.shayan.amro.core.ui.designsystem.icon.Glyphs
import com.shayan.amro.core.ui.text.AmroText
import com.shayan.amro.feature.discover.R
import com.shayan.amro.feature.discover.listLoadStates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** How many rows a list has when the case under test is about there being some. */
private const val ROW_COUNT = 3

private val SETTLED = LoadState.NotLoading(endOfPaginationReached = true)

class DiscoverUiStateMapperTest {
    @Test
    fun `a first read of a list nothing is cached for is the skeleton`() {
        val body = discoverBody(loadState = listLoadStates(LoadState.Loading), itemCount = 0)

        assertEquals(DiscoverContent.Skeleton, body.content)
        assertNull(body.notice)
        assertTrue(!body.isRefreshing, "a skeleton is already the progress of a first read")
    }

    @Test
    fun `a failed read with nothing cached is the error naming the cause`() {
        val body =
            discoverBody(
                loadState = listLoadStates(failure(DataError.NoConnectivity)),
                itemCount = 0,
            )

        val content = body.content
        assertTrue(content is DiscoverContent.Error, "expected an error, was $content")
        assertEquals(Glyphs.CLOUD_OFF, content.glyph)
        assertEquals(
            AmroText.Resource(R.string.feature_discover_error_no_connectivity),
            content.title,
        )
        assertNull(body.notice)
    }

    @Test
    fun `a failed read over rows already on screen is a notice, and keeps them`() {
        val body =
            discoverBody(
                loadState = listLoadStates(failure(DataError.Server)),
                itemCount = ROW_COUNT,
            )

        assertEquals(DiscoverContent.Movies, body.content)
        val notice = assertNotNull(body.notice)
        assertEquals(Glyphs.ERROR, notice.glyph)
        assertEquals(AmroText.Resource(R.string.feature_discover_notice_server), notice.message)
    }

    @Test
    fun `a failure the data layer never raised still reads as a cause the screen has words for`() {
        val body =
            discoverBody(
                loadState = listLoadStates(LoadState.Error(IllegalStateException())),
                itemCount = 0,
            )

        val content = body.content
        assertTrue(content is DiscoverContent.Error, "expected an error, was $content")
        assertEquals(AmroText.Resource(R.string.feature_discover_error_server), content.title)
    }

    @Test
    fun `a settled read with the movies still on their way is the skeleton`() {
        val body =
            discoverBody(
                loadState = listLoadStates(SETTLED, servedEverything = false),
                itemCount = 0,
            )

        assertEquals(
            DiscoverContent.Skeleton,
            body.content,
            "rows a fetch wrote reach the screen a read later, so this is not an empty list",
        )
    }

    @Test
    fun `a catalogue that answered nothing is the empty state rather than an error`() {
        val body = discoverBody(loadState = listLoadStates(SETTLED), itemCount = 0)

        assertEquals(DiscoverContent.EmptyFromFilter, body.content)
        assertNull(body.notice)
    }

    @Test
    fun `a fetch over rows already on screen shows the indicator and no notice`() {
        val body =
            discoverBody(loadState = listLoadStates(LoadState.Loading), itemCount = ROW_COUNT)

        assertEquals(DiscoverContent.Movies, body.content)
        assertTrue(body.isRefreshing)
        assertNull(body.notice)
    }

    @Test
    fun `the badge counts the selected genres plus a sort away from the default`() {
        val counts =
            listOf(
                MovieSelection(),
                MovieSelection(sort = MovieSort(SortKey.TITLE, SortDirection.DESCENDING)),
                MovieSelection(genres = setOf(Genre.COMEDY, Genre.HORROR)),
                MovieSelection(
                    genres = setOf(Genre.COMEDY, Genre.HORROR),
                    sort = MovieSort(SortKey.TITLE, SortDirection.DESCENDING),
                ),
            ).map { it.toDiscoverUiState().activeSelectionCount }

        assertEquals(listOf(0, 1, 2, 3), counts)
    }

    @Test
    fun `the sheet offers every genre, every key and every direction`() {
        val filter = MovieSelection(genres = setOf(Genre.COMEDY)).toDiscoverUiState().filter

        assertEquals(Genre.entries.map { it.name }, filter.genres.map { it.value })
        assertEquals(SortKey.entries, filter.sortKeys.map { it.value })
        assertEquals(SortDirection.entries.toSet(), filter.directions.map { it.value }.toSet())
        assertEquals(
            listOf(Genre.COMEDY.name),
            filter.genres.filter { it.isSelected }.map { it.value },
        )
    }

    @Test
    fun `a movie the source has no poster for is a row with no poster to load`() {
        val withPoster = MovieFixture.movie()
        val withoutPoster = MovieFixture.movie(poster = null)

        assertEquals(withPoster.poster?.small, withPoster.toRow().posterUrl)
        assertNull(withoutPoster.toRow().posterUrl)
    }
}

private fun failure(cause: DataError) = LoadState.Error(DataErrorException(cause))
