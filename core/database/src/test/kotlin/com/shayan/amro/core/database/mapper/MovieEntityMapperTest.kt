package com.shayan.amro.core.database.mapper

import com.shayan.amro.core.model.Genre
import com.shayan.amro.core.model.Rating
import com.shayan.amro.core.model.ReleaseStatus
import com.shayan.amro.core.testing.fixture.model.MovieFixture.detail
import com.shayan.amro.core.testing.fixture.model.MovieFixture.movie
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val RATING_AVERAGE = 7.4
private const val RATING_COUNT = 1284
private const val RETIRED_GENRE_NAME = "SILENT"
private const val RETIRED_STATUS_NAME = "SHELVED"
private const val NO_GENRES = ""

class MovieEntityMapperTest {
    @Test
    fun `every genre survives a round trip`() {
        val movie = movie(genres = Genre.entries)

        assertEquals(Genre.entries.toList(), movie.toEntity().toMovie().genres)
    }

    @Test
    fun `a genre name the app no longer carries is dropped`() {
        val stored = movie().toEntity().copy(genres = "${Genre.HORROR.name},$RETIRED_GENRE_NAME")

        assertEquals(listOf(Genre.HORROR), stored.toMovie().genres)
    }

    @Test
    fun `a movie with no genres round-trips with none`() {
        val movie = movie(genres = emptyList())

        assertEquals(NO_GENRES, movie.toEntity().genres)
        assertEquals(emptyList(), movie.toEntity().toMovie().genres)
    }

    @Test
    fun `a status name the app no longer carries reads as unknown`() {
        val stored = detail().toEntity().copy(status = RETIRED_STATUS_NAME)

        assertEquals(ReleaseStatus.UNKNOWN, stored.toMovieDetail().status)
    }

    @Test
    fun `every status survives a round trip`() {
        ReleaseStatus.entries.forEach { status ->
            val detail = detail(status = status)

            assertEquals(status, detail.toEntity().toMovieDetail().status)
        }
    }

    @Test
    fun `a rating round-trips with the count behind it`() {
        val detail = detail(rating = Rating(average = RATING_AVERAGE, count = RATING_COUNT))

        assertEquals(
            Rating(average = RATING_AVERAGE, count = RATING_COUNT),
            detail.toEntity().toMovieDetail().rating,
        )
    }

    @Test
    fun `a rating with no count round-trips without one`() {
        val detail = detail(rating = Rating(average = RATING_AVERAGE, count = null))

        assertEquals(
            Rating(average = RATING_AVERAGE, count = null),
            detail.toEntity().toMovieDetail().rating,
        )
    }

    @Test
    fun `no rating round-trips as no rating`() {
        val detail = detail(rating = null)

        assertNull(detail.toEntity().toMovieDetail().rating)
    }

    @Test
    fun `a movie with no poster, no date and no genres round-trips with those absent`() {
        val movie = movie(genres = emptyList(), releaseDate = null, poster = null)

        assertEquals(movie, movie.toEntity().toMovie())
    }

    /**
     * The shared columns are declared on both entities, so this and the detail round trip below are
     * what fail if the two ever drift.
     */
    @Test
    fun `a complete movie round-trips unchanged`() {
        val movie = movie()

        assertEquals(movie, movie.toEntity().toMovie())
    }

    @Test
    fun `a complete detail round-trips unchanged`() {
        val detail = detail()

        assertEquals(detail, detail.toEntity().toMovieDetail())
    }

    @Test
    fun `a detail with no tagline, no financials, no runtime and no imdb id round-trips null`() {
        val detail =
            detail(
                tagline = null,
                budget = null,
                revenue = null,
                runtime = null,
                imdbId = null,
            )

        assertEquals(detail, detail.toEntity().toMovieDetail())
    }
}
